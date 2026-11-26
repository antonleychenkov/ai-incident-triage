package ru.leychenkov.triage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.leychenkov.triage.event.RawErrorEvent;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Value("${app.kafka.topics.raw-errors}")
    private String rawErrorsTopic;

    public void send(RawErrorEvent rawEvent) {
        String hiddenStackTrace = hideSecrets(rawEvent.getStackTrace());
        String hiddenMessage = hideSecrets(rawEvent.getErrorMessage());

        RawErrorEvent sanitized = RawErrorEvent.builder()
                .serviceName(rawEvent.getServiceName())
                .environment(rawEvent.getEnvironment())
                .exceptionClass(rawEvent.getExceptionClass())
                .errorMessage(hiddenMessage)
                .stackTrace(hiddenStackTrace)
                .traceId(rawEvent.getTraceId())
                .occurredAt(rawEvent.getOccurredAt() != null ? rawEvent.getOccurredAt() : Instant.now())
                .build();

        kafkaTemplate.send(rawErrorsTopic, sanitized.getServiceName(), sanitized);
    }

    private String hideSecrets(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replaceAll("(?i)(password|secret|token|api_key)=[^&\\s]+", "$1=***")
                .replaceAll("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", "Bearer ***")
                .replaceAll("\\b\\d{1,3}(\\.\\d{1,3}){3}\\b", "***.***.***.***");
    }
}