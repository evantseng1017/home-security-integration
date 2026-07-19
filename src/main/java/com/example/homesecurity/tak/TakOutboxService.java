package com.example.homesecurity.tak;

import com.example.homesecurity.event.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TakOutboxService {
    private static final Logger log = LoggerFactory.getLogger(TakOutboxService.class);
    private final TakOutboxRepository repository;
    private final CotConverter converter;
    private final TakTransport transport;
    private final TakProperties properties;

    public TakOutboxService(TakOutboxRepository repository, CotConverter converter,
                            TakTransport transport, TakProperties properties) {
        this.repository = repository;
        this.converter = converter;
        this.transport = transport;
        this.properties = properties;
    }

    public void enqueue(SecurityEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            repository.save(new TakOutboxMessage(event.getEventId(), converter.convert(event)));
        } catch (DataIntegrityViolationException ignored) {
            log.debug("TAK event {} is already queued", event.getEventId());
        }
    }

    @Scheduled(fixedDelayString = "${tak.retry-interval:1000}")
    @Transactional
    public void deliverPending() {
        if (!properties.isEnabled()) {
            return;
        }
        List<TakOutboxMessage> messages = repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(
                Instant.now(), PageRequest.of(0, properties.getBatchSize()));
        for (TakOutboxMessage message : messages) {
            try {
                transport.send(message.getPayload());
                repository.delete(message);
                log.info("Delivered security event {} to TAK", message.getEventId());
            } catch (Exception error) {
                Duration delay = retryDelay(message.getAttempts());
                message.failed(error, Instant.now().plus(delay));
                log.warn("TAK delivery failed for {}; retrying in {} seconds: {}",
                        message.getEventId(), delay.toSeconds(), error.getMessage());
            }
        }
    }

    private Duration retryDelay(int attempts) {
        long seconds = Math.min(300, 1L << Math.min(attempts, 8));
        return Duration.ofSeconds(seconds);
    }
}
