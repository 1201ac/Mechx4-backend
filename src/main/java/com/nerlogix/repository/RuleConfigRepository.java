package com.nerlogix.repository;

import com.nerlogix.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {
    Optional<RuleConfig> findByConfigKey(String configKey);
}
