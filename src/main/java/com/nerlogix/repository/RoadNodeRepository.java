package com.nerlogix.repository;

import com.nerlogix.entity.RoadNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface RoadNodeRepository extends JpaRepository<RoadNode, Long> {
    Optional<RoadNode> findByName(String name);
    List<RoadNode> findByDistrictId(Long districtId);
}
