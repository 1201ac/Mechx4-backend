package com.nerlogix.repository;

import com.nerlogix.entity.Incident;
import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByStatusNot(IncidentStatus status);
    List<Incident> findBySegmentIdAndStatus(Long segmentId, IncidentStatus status);
    List<Incident> findByTypeAndStatus(IncidentType type, IncidentStatus status);
    List<Incident> findByTypeAndStatusNotAndCreatedAtAfter(IncidentType type, IncidentStatus status, java.time.LocalDateTime after);
}
