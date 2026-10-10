package com.nerlogix.repository;

import com.nerlogix.entity.DistrictStock;
import com.nerlogix.model.enums.CargoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DistrictStockRepository extends JpaRepository<DistrictStock, Long> {
    List<DistrictStock> findByDistrictId(Long districtId);
    Optional<DistrictStock> findByDistrictIdAndCategory(Long districtId, CargoCategory category);
}
