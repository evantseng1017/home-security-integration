#pragma once

#define SECURITY_WIFI_SSID "your-wifi-name"
#define SECURITY_WIFI_PASSWORD "your-wifi-password"

// Use the LAN IPv4 address of the computer running Spring Boot. An ESP32
// cannot use localhost to reach another computer.
#define SECURITY_API_URL "http://192.168.1.100:8080/api/events"

#define SECURITY_DEVICE_ID "esp32-front-door"
#define SECURITY_LOCATION_NAME "Front Door"
#define SECURITY_LATITUDE 39.7392
#define SECURITY_LONGITUDE -104.9903
