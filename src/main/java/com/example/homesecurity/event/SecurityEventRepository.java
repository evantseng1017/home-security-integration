package com.example.homesecurity.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, String> {
    List<SecurityEvent> findAllByOrderByTimestampDesc();
}
