#pragma once

#include <cstddef>

// Create include/secrets.h to override these development defaults without
// committing credentials. See include/secrets.example.h.
#if __has_include("secrets.h")
#include "secrets.h"
#endif

#ifndef SECURITY_WIFI_SSID
#define SECURITY_WIFI_SSID "CHANGE_ME"
#endif

#ifndef SECURITY_WIFI_PASSWORD
#define SECURITY_WIFI_PASSWORD "CHANGE_ME"
#endif

#ifndef SECURITY_API_URL
#define SECURITY_API_URL "http://192.168.1.100:8080/api/events"
#endif

#ifndef SECURITY_DEVICE_ID
#define SECURITY_DEVICE_ID "esp32-security-node-01"
#endif

#ifndef SECURITY_LOCATION_NAME
#define SECURITY_LOCATION_NAME "Front Door"
#endif

#ifndef SECURITY_LATITUDE
#define SECURITY_LATITUDE 39.7392
#endif

#ifndef SECURITY_LONGITUDE
#define SECURITY_LONGITUDE -104.9903
#endif

namespace config {
inline constexpr int pirPin = 27;
inline constexpr unsigned long motionCooldownMs = 5000;
inline constexpr unsigned long wifiReconnectMs = 10000;
inline constexpr unsigned long initialRetryMs = 2000;
inline constexpr unsigned long maximumRetryMs = 60000;
inline constexpr unsigned long ntpWaitMs = 15000;
inline constexpr std::size_t queueCapacity = 16;
}  // namespace config
