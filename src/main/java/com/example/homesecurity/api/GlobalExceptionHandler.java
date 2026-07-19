package com.example.homesecurity.api;

import com.example.homesecurity.event.DuplicateEventException;
import com.example.homesecurity.event.EventNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                               HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException ex,
                                                  HttpServletRequest request) {
        String message = "Malformed JSON request";
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null && cause.getMessage().contains("EventType")) {
            message = "Unknown eventType. Use one of: MOTION_DETECTED, ACCESS_GRANTED, ACCESS_DENIED, "
                    + "SYSTEM_ARMED, SYSTEM_DISARMED, PANIC_BUTTON, DEVICE_ONLINE, DEVICE_OFFLINE";
        }
        return response(HttpStatus.BAD_REQUEST, message, request, Map.of());
    }

    @ExceptionHandler(DuplicateEventException.class)
    ResponseEntity<ApiError> handleDuplicate(DuplicateEventException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EventNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(EventNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message,
                                               HttpServletRequest request,
                                               Map<String, String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
