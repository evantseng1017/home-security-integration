package com.example.homesecurity.tak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tak_outbox")
public class TakOutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 1000)
    private String lastError;

    protected TakOutboxMessage() { }

    TakOutboxMessage(String eventId, String payload) {
        this.eventId = eventId;
        this.payload = payload;
        this.nextAttemptAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }

    void failed(Exception error, Instant retryAt) {
        attempts++;
        nextAttemptAt = retryAt;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }
}
