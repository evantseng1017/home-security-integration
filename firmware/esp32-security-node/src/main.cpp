#include <Arduino.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <HTTPClient.h>
#include <WiFi.h>
#include <esp_system.h>
#include <time.h>

#include <array>
#include <cstdio>
#include <string>

#include "DeviceConfig.h"
#include "EventCodec.h"
#include "SecurityEvent.h"

namespace {
constexpr char bleServiceUuid[] = "5a5b1000-31d4-4c21-9f1b-df4a686fbd11";
constexpr char latestEventUuid[] = "5a5b1001-31d4-4c21-9f1b-df4a686fbd11";
constexpr char armedStateUuid[] = "5a5b1002-31d4-4c21-9f1b-df4a686fbd11";
constexpr char deviceHealthUuid[] = "5a5b1003-31d4-4c21-9f1b-df4a686fbd11";

std::array<SecurityEvent, config::queueCapacity> eventQueue;
std::size_t queueHead = 0;
std::size_t queueSize = 0;
bool armed = true;
bool previousMotion = false;
unsigned long lastMotionMs = 0;
unsigned long lastWifiAttemptMs = 0;
unsigned long nextDeliveryAttemptMs = 0;
unsigned long retryDelayMs = config::initialRetryMs;
BLECharacteristic* latestEventCharacteristic = nullptr;
BLECharacteristic* armedStateCharacteristic = nullptr;
BLECharacteristic* healthCharacteristic = nullptr;

void logLine(const char* level, const String& message) {
    Serial.printf("{\"level\":\"%s\",\"uptimeMs\":%lu,\"message\":\"%s\"}\n",
                  level, millis(), message.c_str());
}

std::string utcTimestamp() {
    const time_t now = time(nullptr);
    if (now < 1700000000) {
        return "1970-01-01T00:00:00Z";
    }
    tm utc{};
    gmtime_r(&now, &utc);
    char buffer[25];
    strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%SZ", &utc);
    return buffer;
}

std::string createEventId() {
    char buffer[64];
    const uint64_t chipId = ESP.getEfuseMac();
    snprintf(buffer, sizeof(buffer), "%04X%08X-%08lX-%08lX",
             static_cast<uint16_t>(chipId >> 32),
             static_cast<uint32_t>(chipId), millis(), esp_random());
    return buffer;
}

SecurityEvent createEvent(const char* eventType) {
    return SecurityEvent{
        createEventId(), SECURITY_DEVICE_ID, eventType, SECURITY_LOCATION_NAME,
        SECURITY_LATITUDE, SECURITY_LONGITUDE, armed, utcTimestamp()
    };
}

void publishBleState(const std::string& eventJson = {}) {
    if (armedStateCharacteristic != nullptr) {
        armedStateCharacteristic->setValue(armed ? "true" : "false");
    }
    if (healthCharacteristic != nullptr) {
        const std::string health = WiFi.status() == WL_CONNECTED ? "ONLINE" : "OFFLINE";
        healthCharacteristic->setValue(health.c_str());
        healthCharacteristic->notify();
    }
    if (!eventJson.empty() && latestEventCharacteristic != nullptr) {
        latestEventCharacteristic->setValue(eventJson.c_str());
        latestEventCharacteristic->notify();
    }
}

bool enqueueEvent(const SecurityEvent& event) {
    if (queueSize == eventQueue.size()) {
        logLine("ERROR", "Event queue full; dropping newest event");
        return false;
    }
    const std::size_t tail = (queueHead + queueSize) % eventQueue.size();
    eventQueue[tail] = event;
    ++queueSize;
    const std::string json = encodeEventJson(event);
    Serial.printf("EVENT %s\n", json.c_str());
    publishBleState(json);
    return true;
}

void removeQueuedEvent() {
    if (queueSize == 0) return;
    queueHead = (queueHead + 1) % eventQueue.size();
    --queueSize;
}

void connectWifi() {
    if (WiFi.status() == WL_CONNECTED) return;
    if (millis() - lastWifiAttemptMs < config::wifiReconnectMs) return;
    lastWifiAttemptMs = millis();
    logLine("INFO", "Connecting to Wi-Fi");
    WiFi.mode(WIFI_STA);
    WiFi.begin(SECURITY_WIFI_SSID, SECURITY_WIFI_PASSWORD);
}

bool deliverEvent(const SecurityEvent& event) {
    if (WiFi.status() != WL_CONNECTED) return false;
    HTTPClient http;
    http.setConnectTimeout(5000);
    http.setTimeout(5000);
    if (!http.begin(SECURITY_API_URL)) {
        logLine("ERROR", "Unable to initialize HTTP request");
        return false;
    }
    http.addHeader("Content-Type", "application/json");
    const std::string json = encodeEventJson(event);
    const int status = http.POST(String(json.c_str()));
    http.end();
    if (status == 201 || status == 409) {
        Serial.printf("DELIVERED status=%d eventId=%s\n", status, event.eventId.c_str());
        return true;
    }
    Serial.printf("DELIVERY_FAILED status=%d eventId=%s\n", status, event.eventId.c_str());
    return false;
}

void processDeliveryQueue() {
    if (queueSize == 0 || millis() < nextDeliveryAttemptMs) return;
    if (deliverEvent(eventQueue[queueHead])) {
        removeQueuedEvent();
        retryDelayMs = config::initialRetryMs;
        nextDeliveryAttemptMs = millis();
    } else {
        nextDeliveryAttemptMs = millis() + retryDelayMs + (esp_random() % 500);
        retryDelayMs = min(retryDelayMs * 2, config::maximumRetryMs);
    }
}

void setArmed(bool newState) {
    if (armed == newState) return;
    armed = newState;
    enqueueEvent(createEvent(armed ? "SYSTEM_ARMED" : "SYSTEM_DISARMED"));
}

class ArmedStateCallbacks final : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) override {
        const std::string value = characteristic->getValue();
        if (value == "true" || value == "1" || value == "ARM") setArmed(true);
        if (value == "false" || value == "0" || value == "DISARM") setArmed(false);
    }
};

void startBle() {
    BLEDevice::init(SECURITY_DEVICE_ID);
    BLEServer* server = BLEDevice::createServer();
    BLEService* service = server->createService(bleServiceUuid);
    latestEventCharacteristic = service->createCharacteristic(
        latestEventUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    latestEventCharacteristic->addDescriptor(new BLE2902());
    armedStateCharacteristic = service->createCharacteristic(
        armedStateUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
    armedStateCharacteristic->setCallbacks(new ArmedStateCallbacks());
    healthCharacteristic = service->createCharacteristic(
        deviceHealthUuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    healthCharacteristic->addDescriptor(new BLE2902());
    service->start();
    BLEAdvertising* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(bleServiceUuid);
    advertising->setScanResponse(true);
    BLEDevice::startAdvertising();
    publishBleState();
    logLine("INFO", "BLE GATT service started");
}

void processSerialCommand() {
    if (!Serial.available()) return;
    String command = Serial.readStringUntil('\n');
    command.trim();
    command.toUpperCase();
    if (command == "PING") {
        Serial.println("ACK PONG");
    } else if (command == "STATUS") {
        Serial.printf("STATUS armed=%s wifi=%s queued=%u\n", armed ? "true" : "false",
                      WiFi.status() == WL_CONNECTED ? "connected" : "disconnected",
                      static_cast<unsigned>(queueSize));
    } else if (command == "ARM") {
        setArmed(true);
        Serial.println("ACK ARM");
    } else if (command == "DISARM") {
        setArmed(false);
        Serial.println("ACK DISARM");
    } else if (!command.isEmpty()) {
        Serial.printf("NACK unknown_command=%s\n", command.c_str());
    }
}
}  // namespace

void setup() {
    Serial.begin(115200);
    Serial.setTimeout(50);
    pinMode(config::pirPin, INPUT);
    logLine("INFO", "Security node booting");
    connectWifi();
    configTime(0, 0, "pool.ntp.org", "time.nist.gov");
    const unsigned long startedWaiting = millis();
    while (time(nullptr) < 1700000000 && millis() - startedWaiting < config::ntpWaitMs) {
        delay(100);
    }
    startBle();
    enqueueEvent(createEvent("DEVICE_ONLINE"));
}

void loop() {
    connectWifi();
    processSerialCommand();

    const bool motion = digitalRead(config::pirPin) == HIGH;
    if (armed && motion && !previousMotion && millis() - lastMotionMs >= config::motionCooldownMs) {
        lastMotionMs = millis();
        enqueueEvent(createEvent("MOTION_DETECTED"));
    }
    previousMotion = motion;
    processDeliveryQueue();
    delay(10);
}
