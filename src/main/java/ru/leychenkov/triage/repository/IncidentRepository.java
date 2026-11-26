package ru.leychenkov.triage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.leychenkov.triage.entity.Incident;
import ru.leychenkov.triage.entity.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByFingerprint(String fingerprint);

    List<Incident> findAllByOrderByLastSeenAtDesc();

    List<Incident> findAllByStatusOrderByLastSeenAtDesc(Status status);

    @Modifying
    @Query("UPDATE Incident i SET i.occurrences = i.occurrences + 1, i.lastSeenAt = :now WHERE i.id = :id")
    void incrementOccurrences(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Incident i SET i.status = :status WHERE i.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Status status);

    @Query(value = """
        INSERT INTO incidents (
            fingerprint, service_name, exception_class, error_message,
            sample_stacktrace, status, occurrences, first_seen_at, last_seen_at
        )
        VALUES (
            :fingerprint, :serviceName, :exceptionClass, :errorMessage,
            :sampleStacktrace, 'NEW', 1, :now, :now
        )
        ON CONFLICT (fingerprint) DO UPDATE
        SET occurrences = incidents.occurrences + 1,
            last_seen_at = EXCLUDED.last_seen_at
        RETURNING *
        """, nativeQuery = true)
    Incident upsertIncident(
            @Param("fingerprint") String fingerprint,
            @Param("serviceName") String serviceName,
            @Param("exceptionClass") String exceptionClass,
            @Param("errorMessage") String errorMessage,
            @Param("sampleStacktrace") String sampleStacktrace,
            @Param("now") LocalDateTime now
    );
}