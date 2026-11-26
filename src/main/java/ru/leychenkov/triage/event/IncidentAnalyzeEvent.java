package ru.leychenkov.triage.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAnalyzeEvent {

    private Long incidentId;

    private String serviceName;

    private String exceptionClass;

    private String errorMessage;

    private String cleanStackTrace;
}