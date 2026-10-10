package com.nerlogix.repository;

import com.nerlogix.entity.FieldReport;
import com.nerlogix.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FieldReportRepository extends JpaRepository<FieldReport, Long> {
    Optional<FieldReport> findByClientId(String clientId);
    List<FieldReport> findByUserId(Long userId);
    Page<FieldReport> findByUserId(Long userId, Pageable pageable);
    List<FieldReport> findByStatus(ReportStatus status);
}
