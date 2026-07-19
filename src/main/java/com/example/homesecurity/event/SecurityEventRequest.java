package com.example.homesecurity.event;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SecurityEventRequest(
        @NotBlank @Size(max = 100) String eventId,
        @NotBlank @Size(max = 100) String deviceId,
        @NotNull EventType eventType,
        @NotBlank @Size(max = 200) String locationName,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull Boolean armed,
        @NotNull Instant timestamp
) {
    SecurityEvent toEntity() {
        return new SecurityEvent(eventId, deviceId, eventType, locationName,
                latitude, longitude, armed, timestamp);
    }
}
