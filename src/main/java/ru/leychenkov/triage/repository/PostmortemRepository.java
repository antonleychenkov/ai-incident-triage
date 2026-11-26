package ru.leychenkov.triage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.leychenkov.triage.entity.Postmortem;

import java.util.Optional;

@Repository
public interface PostmortemRepository extends JpaRepository<Postmortem, Long> {

    Optional<Postmortem> findByIncidentId(Long incidentId);
}