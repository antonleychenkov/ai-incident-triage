package ru.leychenkov.triage.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fingerprint", nullable = false, unique = true, length = 64)
    private String fingerprint;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "exception_class", nullable = false)
    private String exceptionClass;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sample_stacktrace", nullable = false, columnDefinition = "TEXT")
    private String sampleStacktrace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Column(name = "ai_root_cause", columnDefinition = "TEXT")
    private String aiRootCause;

    @Column(name = "ai_suggested_fix", columnDefinition = "TEXT")
    private String aiSuggestedFix;

    @Column(name = "occurrences", nullable = false)
    private Long occurrences = 1L;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt = LocalDateTime.now();
}
