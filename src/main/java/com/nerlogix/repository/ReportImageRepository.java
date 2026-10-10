package com.nerlogix.repository;

import com.nerlogix.entity.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    List<ReportImage> findByFieldReportId(Long fieldReportId);
}
