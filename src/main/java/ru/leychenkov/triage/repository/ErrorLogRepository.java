package ru.leychenkov.triage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.leychenkov.triage.entity.ErrorLog;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}