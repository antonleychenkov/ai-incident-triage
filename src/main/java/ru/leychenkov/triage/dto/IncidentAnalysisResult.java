package ru.leychenkov.triage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.leychenkov.triage.entity.Severity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAnalysisResult {

    private Severity severity;
    private String rootCause;
    private String suggestedFix;
}