package com.example.homesecurity.event;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String eventId) {
        super("An event with eventId '" + eventId + "' already exists");
    }
}
