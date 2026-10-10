package com.nerlogix.repository;

import com.nerlogix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleAndActive(com.nerlogix.model.enums.Role role, boolean active);
}
