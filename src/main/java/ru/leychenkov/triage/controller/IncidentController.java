package ru.leychenkov.triage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.leychenkov.triage.entity.Incident;
import ru.leychenkov.triage.entity.Status;
import ru.leychenkov.triage.event.RawErrorEvent;
import ru.leychenkov.triage.exception.IncidentNotFoundException;
import ru.leychenkov.triage.repository.IncidentRepository;
import ru.leychenkov.triage.service.ErrorProducerService;
import ru.leychenkov.triage.service.PostmortemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IncidentController {

    private final ErrorProducerService errorProducerService;
    private final IncidentRepository incidentRepository;
    private final PostmortemService postmortemService;

    @PostMapping("/errors")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveError(@Valid @RequestBody RawErrorEvent event) {
        errorProducerService.send(event);
    }

    @GetMapping("/incidents")
    public List<Incident> getIncidents(@RequestParam(required = false) Status status) {
        if (status != null) {
            return incidentRepository.findAllByStatusOrderByLastSeenAtDesc(status);
        }
        return incidentRepository.findAllByOrderByLastSeenAtDesc();
    }

    @GetMapping("/incidents/{id}")
    public Incident getIncidentById(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Инцидент с ID " + id + " не найден"));
    }

    @PatchMapping("/incidents/{id}/status")
    public void updateStatus(@PathVariable Long id, @RequestParam Status status) {
        if (!incidentRepository.existsById(id)) {
            throw new IncidentNotFoundException("Инцидент с ID " + id + " не найден");
        }
        incidentRepository.updateStatus(id, status);
    }

    @PostMapping(value = "/incidents/{id}/postmortem", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String generatePostmortem(@PathVariable Long id) {
        return postmortemService.generateOrGetPostmortem(id);
    }
}