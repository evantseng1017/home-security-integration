package com.example.homesecurity.tak;

import com.example.homesecurity.event.EventType;

import java.util.Map;

final class CotTypeMapping {
    private static final Map<EventType, String> TYPES = Map.of(
            EventType.MOTION_DETECTED, "b-m-p-s-m",
            EventType.ACCESS_GRANTED, "b-m-p-s-a-g",
            EventType.ACCESS_DENIED, "b-m-p-s-a-d",
            EventType.SYSTEM_ARMED, "b-m-p-s-s-a",
            EventType.SYSTEM_DISARMED, "b-m-p-s-s-d",
            EventType.PANIC_BUTTON, "b-m-p-s-p",
            EventType.DEVICE_ONLINE, "b-m-p-s-d-o",
            EventType.DEVICE_OFFLINE, "b-m-p-s-d-f"
    );

    private CotTypeMapping() { }

    static String forEvent(EventType eventType) {
        return TYPES.get(eventType);
    }
}
