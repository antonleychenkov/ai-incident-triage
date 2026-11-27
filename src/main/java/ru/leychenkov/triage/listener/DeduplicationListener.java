package ru.leychenkov.triage.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.leychenkov.triage.entity.ErrorLog;
import ru.leychenkov.triage.entity.Incident;
import ru.leychenkov.triage.entity.Status;
import ru.leychenkov.triage.event.IncidentAnalyzeEvent;
import ru.leychenkov.triage.event.RawErrorEvent;
import ru.leychenkov.triage.repository.ErrorLogRepository;
import ru.leychenkov.triage.repository.IncidentRepository;
import ru.leychenkov.triage.service.ErrorProducerService;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeduplicationListener {

    private final StringRedisTemplate redisTemplate;
    private final IncidentRepository incidentRepository;
    private final ErrorLogRepository errorLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.incidents-to-analyze}")
    private String analyzeTopic;

    @Value("${app.triage.cache-ttl-minutes:15}")
    private long cacheTtlMinutes;

    @KafkaListener(topics = "${app.kafka.topics.raw-errors}", groupId = "${app.kafka.groups.deduplication}")
    @Transactional
    public void handleRawError(RawErrorEvent event) {
        String fingerprint = calculateFingerprint(
                event.getServiceName(),
                event.getExceptionClass(),
                event.getStackTrace()
        );
        String redisKey = "incident:fp:" + fingerprint;
        LocalDateTime now = LocalDateTime.now();


        String cachedIncidentId = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.hasText(cachedIncidentId)) {
            Long incidentId = Long.parseLong(cachedIncidentId);
            incidentRepository.incrementOccurrences(incidentId, now);
            saveErrorLog(incidentId, event, now);
            log.debug("Дубликат ошибки (fingerprint={}), счётчик инцидента #{} увеличен", fingerprint, incidentId);
            return;
        }


        Incident incident = incidentRepository.upsertIncident(
                fingerprint,
                event.getServiceName(),
                event.getExceptionClass(),
                event.getErrorMessage(),
                event.getStackTrace(),
                now
        );

        redisTemplate.opsForValue().set(redisKey, incident.getId().toString(), Duration.ofMinutes(cacheTtlMinutes));
        saveErrorLog(incident.getId(), event, now);


        if (incident.getStatus() == Status.NEW && incident.getOccurrences() == 1L) {
            IncidentAnalyzeEvent analyzeEvent = IncidentAnalyzeEvent.builder()
                    .incidentId(incident.getId())
                    .serviceName(incident.getServiceName())
                    .exceptionClass(incident.getExceptionClass())
                    .errorMessage(incident.getErrorMessage())
                    .cleanStackTrace(incident.getSampleStacktrace())
                    .build();

            kafkaTemplate.send(analyzeTopic, incident.getId().toString(), analyzeEvent);
        }
    }

    private void saveErrorLog(Long incidentId, RawErrorEvent event, LocalDateTime now) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setIncidentId(incidentId);
        errorLog.setEnvironment(event.getEnvironment().trim().toLowerCase());
        errorLog.setTraceId(event.getTraceId());
        errorLog.setOccurredAt(now);
        errorLogRepository.save(errorLog);
    }

    private String calculateFingerprint(String service, String exceptionClass, String stackTrace) {
        String normalized = stackTrace
                .replaceAll("@[0-9a-fA-F]+", "@HEX")
                .replaceAll(":\\d+\\)", ":LINE)")
                .replaceAll("\\d+", "N");
        return DigestUtils.sha256Hex(service + ":" + exceptionClass + ":" + normalized);
    }
}