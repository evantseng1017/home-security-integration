package com.example.homesecurity.event;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SecurityEventService {

    private final SecurityEventRepository repository;

    public SecurityEventService(SecurityEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SecurityEvent create(SecurityEventRequest request) {
        if (repository.existsById(request.eventId())) {
            throw new DuplicateEventException(request.eventId());
        }
        try {
            return repository.saveAndFlush(request.toEntity());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEventException(request.eventId());
        }
    }

    @Transactional(readOnly = true)
    public List<SecurityEvent> list() {
        return repository.findAllByOrderByTimestampDesc();
    }
}
