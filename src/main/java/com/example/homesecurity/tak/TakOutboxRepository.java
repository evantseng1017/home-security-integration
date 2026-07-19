package com.example.homesecurity.tak;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TakOutboxRepository extends JpaRepository<TakOutboxMessage, Long> {
    List<TakOutboxMessage> findByNextAttemptAtLessThanEqualOrderByIdAsc(Instant now, Pageable pageable);
}
