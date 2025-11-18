package com.thp.sqlsaas.persistence.repository;

import com.thp.sqlsaas.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by userId and tenantId
     */
    Optional<User> findByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Check if user exists
     */
    boolean existsByUserIdAndTenantId(String userId, String tenantId);
}