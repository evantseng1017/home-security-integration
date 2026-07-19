package com.example.homesecurity.event;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String eventId) {
        super("Security event not found: " + eventId);
    }
}
