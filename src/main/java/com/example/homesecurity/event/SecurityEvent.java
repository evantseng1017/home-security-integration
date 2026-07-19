package com.example.homesecurity.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType eventType;

    @Column(nullable = false, length = 200)
    private String locationName;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private boolean armed;

    @Column(nullable = false)
    private Instant timestamp;

    protected SecurityEvent() {
    }

    public SecurityEvent(String eventId, String deviceId, EventType eventType, String locationName,
                         double latitude, double longitude, boolean armed, Instant timestamp) {
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.armed = armed;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public String getDeviceId() { return deviceId; }
    public EventType getEventType() { return eventType; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isArmed() { return armed; }
    public Instant getTimestamp() { return timestamp; }
}
