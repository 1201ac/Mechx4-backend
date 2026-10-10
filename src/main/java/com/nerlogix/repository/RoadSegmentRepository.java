package com.nerlogix.repository;

import com.nerlogix.entity.RoadSegment;
import com.nerlogix.model.enums.SegmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long>, JpaSpecificationExecutor<RoadSegment> {
    List<RoadSegment> findByStatus(SegmentStatus status);
    List<RoadSegment> findByFromNodeIdOrToNodeId(Long fromNodeId, Long toNodeId);
}
