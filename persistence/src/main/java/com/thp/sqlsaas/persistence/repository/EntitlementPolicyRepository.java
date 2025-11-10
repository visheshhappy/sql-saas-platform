package com.thp.sqlsaas.persistence.repository;

import com.thp.sqlsaas.persistence.entity.EntitlementPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing entitlement policies.
 */
@Repository
public interface EntitlementPolicyRepository extends JpaRepository<EntitlementPolicy, Long> {
    
    /**
     * Find all enabled policies for a specific tenant.
     */
    List<EntitlementPolicy> findByTenantIdAndEnabledTrueOrderByPriorityDesc(String tenantId);
    
    /**
     * Find a specific policy by its policy ID.
     */
    Optional<EntitlementPolicy> findByPolicyId(String policyId);
    
    /**
     * Find all policies for a tenant matching source and table patterns.
     * This is a simple implementation - in production, you'd use a more sophisticated
     * pattern matching query.
     */
    @Query("SELECT p FROM EntitlementPolicy p WHERE " +
           "p.tenantId = :tenantId AND " +
           "p.enabled = true AND " +
           "(p.sourcePattern = :sourceId OR p.sourcePattern = '*') AND " +
           "(p.tablePattern = :tableName OR p.tablePattern = '*') " +
           "ORDER BY p.priority DESC")
    List<EntitlementPolicy> findApplicablePolicies(
            @Param("tenantId") String tenantId,
            @Param("sourceId") String sourceId,
            @Param("tableName") String tableName
    );
    
    /**
     * Find all policies by type for a tenant.
     */
    List<EntitlementPolicy> findByTenantIdAndPolicyTypeAndEnabledTrue(
            String tenantId, 
            String policyType
    );
    
    /**
     * Count enabled policies for a tenant.
     */
    long countByTenantIdAndEnabledTrue(String tenantId);
}
