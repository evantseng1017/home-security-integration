#pragma once

#include <string>

struct SecurityEvent {
    std::string eventId;
    std::string deviceId;
    std::string eventType;
    std::string locationName;
    double latitude{};
    double longitude{};
    bool armed{};
    std::string timestamp;
};
