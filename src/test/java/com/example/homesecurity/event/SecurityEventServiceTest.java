package com.example.homesecurity.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {

    @Mock
    private SecurityEventRepository repository;

    @InjectMocks
    private SecurityEventService service;

    @Test
    void rejectsDuplicateBeforeSaving() {
        SecurityEventRequest request = new SecurityEventRequest(
                "event-1", "esp32-front", EventType.MOTION_DETECTED, "Front Door",
                39.7392, -104.9903, true, Instant.parse("2026-07-18T12:00:00Z"));
        when(repository.existsById("event-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("event-1");
        verify(repository).existsById("event-1");
    }
}
