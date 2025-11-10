package com.thp.sqlsaas.persistence.service;

import com.thp.sqlsaas.persistence.entity.EntitlementPolicy;
import com.thp.sqlsaas.persistence.repository.EntitlementPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing entitlement policies.
 */
@Service
public class PolicyService {
    
    @Autowired
    private EntitlementPolicyRepository policyRepository;
    
    /**
     * Get all policies for a tenant (enabled only).
     */
    @Transactional(readOnly = true)
    public List<EntitlementPolicy> getPoliciesForTenant(String tenantId) {
        return policyRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId);
    }
    
    /**
     * Get applicable policies for a specific source and table.
     */
    @Transactional(readOnly = true)
    public List<EntitlementPolicy> getApplicablePolicies(
            String tenantId,
            String sourceId, 
            String tableName) {
        return policyRepository.findApplicablePolicies(tenantId, sourceId, tableName);
    }
    
    /**
     * Get a specific policy by policy ID.
     */
    @Transactional(readOnly = true)
    public Optional<EntitlementPolicy> getPolicyById(String policyId) {
        return policyRepository.findByPolicyId(policyId);
    }
    
    /**
     * Create a new policy.
     */
    @Transactional
    public EntitlementPolicy createPolicy(EntitlementPolicy policy) {
        return policyRepository.save(policy);
    }
    
    /**
     * Update an existing policy.
     */
    @Transactional
    public EntitlementPolicy updatePolicy(EntitlementPolicy policy) {
        return policyRepository.save(policy);
    }
    
    /**
     * Delete a policy.
     */
    @Transactional
    public void deletePolicy(String policyId) {
        policyRepository.findByPolicyId(policyId)
                .ifPresent(policyRepository::delete);
    }
    
    /**
     * Enable or disable a policy.
     */
    @Transactional
    public void setPolicyEnabled(String policyId, boolean enabled) {
        policyRepository.findByPolicyId(policyId).ifPresent(policy -> {
            policy.setEnabled(enabled);
            policyRepository.save(policy);
        });
    }
    
    /**
     * Get count of enabled policies for a tenant.
     */
    @Transactional(readOnly = true)
    public long countEnabledPolicies(String tenantId) {
        return policyRepository.countByTenantIdAndEnabledTrue(tenantId);
    }
}
