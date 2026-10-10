package com.nerlogix.repository;

import com.nerlogix.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);
    Optional<ApiKey> findByKeyHash(String keyHash);
}
