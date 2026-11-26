package ru.leychenkov.triage.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "postmortems")
@Data
public class Postmortem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private Long incidentId;

    @Column(name = "markdown_report", nullable = false, columnDefinition = "TEXT")
    private String markdownReport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}