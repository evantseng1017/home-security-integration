#include "EventCodec.h"

#include <iomanip>
#include <sstream>

std::string escapeJson(const std::string& value) {
    std::ostringstream output;
    for (const unsigned char character : value) {
        switch (character) {
            case '"': output << "\\\""; break;
            case '\\': output << "\\\\"; break;
            case '\b': output << "\\b"; break;
            case '\f': output << "\\f"; break;
            case '\n': output << "\\n"; break;
            case '\r': output << "\\r"; break;
            case '\t': output << "\\t"; break;
            default:
                if (character < 0x20) {
                    output << "\\u" << std::hex << std::setw(4)
                           << std::setfill('0') << static_cast<int>(character)
                           << std::dec;
                } else {
                    output << character;
                }
        }
    }
    return output.str();
}

std::string encodeEventJson(const SecurityEvent& event) {
    std::ostringstream json;
    json << std::fixed << std::setprecision(6)
         << "{\"eventId\":\"" << escapeJson(event.eventId)
         << "\",\"deviceId\":\"" << escapeJson(event.deviceId)
         << "\",\"eventType\":\"" << escapeJson(event.eventType)
         << "\",\"locationName\":\"" << escapeJson(event.locationName)
         << "\",\"latitude\":" << event.latitude
         << ",\"longitude\":" << event.longitude
         << ",\"armed\":" << (event.armed ? "true" : "false")
         << ",\"timestamp\":\"" << escapeJson(event.timestamp) << "\"}";
    return json.str();
}
