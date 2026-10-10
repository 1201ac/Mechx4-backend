package com.nerlogix.repository;

import com.nerlogix.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findByName(String name);
    List<District> findByState(String state);
}
