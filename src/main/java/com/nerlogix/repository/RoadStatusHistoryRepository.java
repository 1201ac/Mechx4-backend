package com.nerlogix.repository;

import com.nerlogix.entity.RoadStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoadStatusHistoryRepository extends JpaRepository<RoadStatusHistory, Long> {
    List<RoadStatusHistory> findBySegmentIdOrderByChangedAtDesc(Long segmentId);
}
