package com.nerlogix.repository;

import com.nerlogix.entity.RefreshToken;
import com.nerlogix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);
    void deleteByFamilyId(String familyId);
    void deleteByTokenHash(String tokenHash);
}
