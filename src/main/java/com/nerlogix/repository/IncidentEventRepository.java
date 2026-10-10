package com.nerlogix.repository;

import com.nerlogix.entity.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {
    List<IncidentEvent> findByIncidentIdOrderByAtDesc(Long incidentId);
    List<IncidentEvent> findAllByOrderByAtDesc();
}
