#pragma once

#include "SecurityEvent.h"

#include <string>

std::string escapeJson(const std::string& value);
std::string encodeEventJson(const SecurityEvent& event);
