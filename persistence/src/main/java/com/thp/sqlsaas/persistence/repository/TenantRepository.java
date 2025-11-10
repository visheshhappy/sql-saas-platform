package com.thp.sqlsaas.persistence.repository;

import com.thp.sqlsaas.persistence.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Tenant entity operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    /**
     * Find a tenant by tenant ID.
     */
    Optional<Tenant> findByTenantId(String tenantId);
    
    /**
     * Check if a tenant exists by tenant ID.
     */
    boolean existsByTenantId(String tenantId);
}
