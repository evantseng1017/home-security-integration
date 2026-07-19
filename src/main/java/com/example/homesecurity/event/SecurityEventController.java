package com.example.homesecurity.event;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.example.homesecurity.tak.CotConverter;

@RestController
@RequestMapping("/api/events")
public class SecurityEventController {

    private final SecurityEventService service;
    private final CotConverter cotConverter;

    public SecurityEventController(SecurityEventService service, CotConverter cotConverter) {
        this.service = service;
        this.cotConverter = cotConverter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityEvent create(@Valid @RequestBody SecurityEventRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<SecurityEvent> list() {
        return service.list();
    }

    @GetMapping(value = "/{eventId}/cot", produces = MediaType.APPLICATION_XML_VALUE)
    public String getCot(@org.springframework.web.bind.annotation.PathVariable String eventId) {
        return cotConverter.convert(service.get(eventId));
    }
}
