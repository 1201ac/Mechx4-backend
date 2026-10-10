package com.nerlogix.repository;

import com.nerlogix.entity.Alert;
import com.nerlogix.model.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findByOpenKey(String openKey);
    List<Alert> findByStatus(AlertStatus status);
}
