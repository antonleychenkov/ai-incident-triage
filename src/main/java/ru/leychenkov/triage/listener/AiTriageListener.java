package ru.leychenkov.triage.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.leychenkov.triage.dto.IncidentAnalysisResult;
import ru.leychenkov.triage.entity.Incident;
import ru.leychenkov.triage.entity.Status;
import ru.leychenkov.triage.event.IncidentAnalyzeEvent;
import ru.leychenkov.triage.repository.IncidentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTriageListener {

    private final ChatClient.Builder chatClientBuilder;
    private final IncidentRepository incidentRepository;


    @Value("${app.triage.max-stacktrace-length:3000}")
    private int maxStacktraceLength;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "${app.kafka.topics.incidents-to-analyze}", groupId = "${app.kafka.groups.ai-triage}")
    @Transactional
    public void analyzeIncident(IncidentAnalyzeEvent event) {
        log.info("Начинаю ИИ-анализ инцидента #{} ({})", event.getIncidentId(), event.getExceptionClass());

        ChatClient chatClient = chatClientBuilder.build();

        IncidentAnalysisResult analysis = chatClient.prompt()
                .system("""
                         Analyze the following Java application exception and stack trace.
                         Determine the severity level (LOW, MEDIUM, HIGH, CRITICAL), root cause, and actionable fix.
                         Provide the rootCause and suggestedFix values in Russian, concisely and technically accurately.
                        """)
                .user(u -> u.text("""
                        Service: {service}
                        Exception: {exception}
                        Message: {message}
                        Stacktrace: {stacktrace}
                        """)
                        .param("service", event.getServiceName())
                        .param("exception", event.getExceptionClass())
                        .param("message", event.getErrorMessage() != null ? event.getErrorMessage() : "N/A")
                        .param("stacktrace", truncateStackTrace(event.getCleanStackTrace(), maxStacktraceLength)))
                .call()
                .entity(IncidentAnalysisResult.class);

        Incident incident = incidentRepository.findById(event.getIncidentId())
                .orElseThrow(() -> new IllegalStateException("Инцидент не найден: " + event.getIncidentId()));

        incident.setSeverity(analysis.getSeverity());
        incident.setAiRootCause(analysis.getRootCause());
        incident.setAiSuggestedFix(analysis.getSuggestedFix());
        incident.setStatus(Status.ANALYZED);

        incidentRepository.save(incident);
    }

    @DltHandler
    @Transactional
    public void handleDlt(IncidentAnalyzeEvent event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error) {
        log.error("Инцидент #{} попал в DLT после всех попыток анализа. Причина: {}", event.getIncidentId(), error);
        incidentRepository.updateStatus(event.getIncidentId(), Status.ANALYSIS_FAILED);
    }

    private String truncateStackTrace(String stackTrace, int maxStacktraceLength) {
        if (stackTrace == null || stackTrace.length() <= maxStacktraceLength) {
            return stackTrace;
        }
        return stackTrace.substring(0, maxStacktraceLength) + "\n... [truncated]";
    }
}