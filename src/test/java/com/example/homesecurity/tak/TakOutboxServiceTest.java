package com.example.homesecurity.tak;

import com.example.homesecurity.event.EventType;
import com.example.homesecurity.event.SecurityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TakOutboxServiceTest {
    @Mock private TakOutboxRepository repository;
    @Mock private CotConverter converter;
    @Mock private TakTransport transport;
    private TakProperties properties;
    private TakOutboxService service;

    @BeforeEach
    void setUp() {
        properties = new TakProperties();
        properties.setEnabled(true);
        service = new TakOutboxService(repository, converter, transport, properties);
    }

    @Test
    void queuesGeneratedCotForAnEvent() {
        SecurityEvent event = event();
        when(converter.convert(event)).thenReturn("<event/>");

        service.enqueue(event);

        verify(repository).save(any(TakOutboxMessage.class));
    }

    @Test
    void removesMessageAfterSuccessfulDelivery() throws Exception {
        TakOutboxMessage message = new TakOutboxMessage("evt-1", "<event/>");
        when(repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(message));

        service.deliverPending();

        verify(transport).send("<event/>");
        verify(repository).delete(message);
    }

    @Test
    void keepsMessageAndSchedulesRetryAfterFailure() throws Exception {
        TakOutboxMessage message = new TakOutboxMessage("evt-1", "<event/>");
        when(repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(message));
        org.mockito.Mockito.doThrow(new IOException("offline")).when(transport).send("<event/>");

        service.deliverPending();

        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getLastError()).isEqualTo("offline");
        assertThat(message.getNextAttemptAt()).isAfter(Instant.now());
        verify(repository, never()).delete(message);
    }

    private SecurityEvent event() {
        return new SecurityEvent("evt-1", "esp32-front", EventType.MOTION_DETECTED,
                "Front Door", 39.7392, -104.9903, true, Instant.parse("2026-07-18T12:00:00Z"));
    }
}
