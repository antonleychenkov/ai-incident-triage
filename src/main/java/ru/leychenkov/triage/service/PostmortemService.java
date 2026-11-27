package ru.leychenkov.triage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.leychenkov.triage.entity.Incident;
import ru.leychenkov.triage.entity.Postmortem;
import ru.leychenkov.triage.exception.IncidentNotFoundException;
import ru.leychenkov.triage.repository.IncidentRepository;
import ru.leychenkov.triage.repository.PostmortemRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostmortemService {

    private final IncidentRepository incidentRepository;
    private final PostmortemRepository postmortemRepository;
    private final ChatClient.Builder chatClientBuilder;

    @Transactional
    public String generateOrGetPostmortem(Long incidentId) {
        return postmortemRepository.findByIncidentId(incidentId)
                .map(Postmortem::getMarkdownReport)
                .orElseGet(() -> createNewPostmortem(incidentId));
    }

    private String createNewPostmortem(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Инцидент с ID " + incidentId + " не найден"));

        log.info("Генерация Postmortem-отчёта для инцидента #{}", incidentId);
        ChatClient chatClient = chatClientBuilder.build();

        String markdown = chatClient.prompt()
                .system("""
                        Write a technical SRE Postmortem report in Markdown format based on the incident data.
                        Include sections: Summary, Timeline, Root Cause, and Action Items.
                        Write the report in Russian, concisely and professionally.
                        """)
                .user(u -> u.text("""
                        Incident ID: {id}
                        Service: {service}
                        Severity: {severity}
                        Exception: {exception}
                        Message: {message}
                        Occurrences: {occurrences}
                        First seen: {firstSeen}
                        Last seen: {lastSeen}
                        AI Root Cause: {rootCause}
                        AI Suggested Fix: {suggestedFix}
                        """)
                        .param("id", incident.getId())
                        .param("service", incident.getServiceName())
                        .param("severity", incident.getSeverity() != null ? incident.getSeverity().name() : "UNKNOWN")
                        .param("exception", incident.getExceptionClass())
                        .param("message", incident.getErrorMessage() != null ? incident.getErrorMessage() : "N/A")
                        .param("occurrences", incident.getOccurrences())
                        .param("firstSeen", incident.getFirstSeenAt())
                        .param("lastSeen", incident.getLastSeenAt())
                        .param("rootCause", incident.getAiRootCause() != null ? incident.getAiRootCause() : "Не определена")
                        .param("suggestedFix", incident.getAiSuggestedFix() != null ? incident.getAiSuggestedFix() : "Не предложено"))
                .call()
                .content();

        Postmortem postmortem = new Postmortem();
        postmortem.setIncidentId(incidentId);
        postmortem.setMarkdownReport(markdown);
        postmortemRepository.save(postmortem);

        return markdown;
    }
}