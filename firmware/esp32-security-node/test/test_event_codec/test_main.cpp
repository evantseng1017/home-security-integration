#include <unity.h>

#include "EventCodec.h"

void test_encodes_backend_contract() {
    const SecurityEvent event{
        "event-1", "esp32-front", "MOTION_DETECTED", "Front Door",
        39.7392, -104.9903, true, "2026-07-18T12:00:00Z"
    };

    TEST_ASSERT_EQUAL_STRING(
        "{\"eventId\":\"event-1\",\"deviceId\":\"esp32-front\","
        "\"eventType\":\"MOTION_DETECTED\",\"locationName\":\"Front Door\","
        "\"latitude\":39.739200,\"longitude\":-104.990300,\"armed\":true,"
        "\"timestamp\":\"2026-07-18T12:00:00Z\"}",
        encodeEventJson(event).c_str());
}

void test_escapes_json_strings() {
    TEST_ASSERT_EQUAL_STRING("Garage \\\"A\\\"\\n", escapeJson("Garage \"A\"\n").c_str());
}

void runTests() {
    UNITY_BEGIN();
    RUN_TEST(test_encodes_backend_contract);
    RUN_TEST(test_escapes_json_strings);
    UNITY_END();
}

#ifdef ARDUINO
#include <Arduino.h>

void setup() {
    delay(2000);
    runTests();
}

void loop() {}
#else
int main(int, char**) {
    runTests();
    return 0;
}
#endif
