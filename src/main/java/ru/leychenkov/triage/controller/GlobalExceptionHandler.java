package ru.leychenkov.triage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.leychenkov.triage.exception.IncidentNotFoundException;
import ru.leychenkov.triage.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(IncidentNotFoundException ex) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "error", ex.getMessage()
        );
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(Exception ex) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "error", ex.getMessage()
        );
    }
}