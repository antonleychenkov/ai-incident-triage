package ru.leychenkov.triage.event;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawErrorEvent {

    @NotBlank
    private String serviceName;

    @NotBlank
    private String environment;

    @NotBlank
    private String exceptionClass;

    private String errorMessage;

    @NotBlank
    private String stackTrace;

    private String traceId;

    private Instant occurredAt;
}