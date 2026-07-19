package com.example.homesecurity.event;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.example.homesecurity.tak.TakOutboxService;

@Service
public class SecurityEventService {

    private final SecurityEventRepository repository;
    private final TakOutboxService takOutboxService;

    public SecurityEventService(SecurityEventRepository repository, TakOutboxService takOutboxService) {
        this.repository = repository;
        this.takOutboxService = takOutboxService;
    }

    @Transactional
    public SecurityEvent create(SecurityEventRequest request) {
        if (repository.existsById(request.eventId())) {
            throw new DuplicateEventException(request.eventId());
        }
        try {
            SecurityEvent event = repository.saveAndFlush(request.toEntity());
            takOutboxService.enqueue(event);
            return event;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEventException(request.eventId());
        }
    }

    @Transactional(readOnly = true)
    public List<SecurityEvent> list() {
        return repository.findAllByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public SecurityEvent get(String eventId) {
        return repository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
