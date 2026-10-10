package com.nerlogix.repository;

import com.nerlogix.entity.WeatherLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WeatherLogRepository extends JpaRepository<WeatherLog, Long> {
    Optional<WeatherLog> findFirstByDistrictIdOrderByLoggedAtDesc(Long districtId);
    List<WeatherLog> findByDistrictIdOrderByLoggedAtDesc(Long districtId);
    List<WeatherLog> findByDistrictIdAndLoggedAtAfterOrderByLoggedAtDesc(Long districtId, LocalDateTime after);
    org.springframework.data.domain.Page<WeatherLog> findAllByOrderByLoggedAtDesc(org.springframework.data.domain.Pageable pageable);
}
