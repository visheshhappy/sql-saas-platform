package com.thp.sqlsaas.persistence.service;

import com.thp.sqlsaas.persistence.entity.Tenant;
import com.thp.sqlsaas.persistence.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing tenants.
 */
@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Create a new tenant.
     */
    public Tenant createTenant(String tenantId, String name) {
        if (tenantRepository.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException("Tenant with ID " + tenantId + " already exists");
        }
        Tenant tenant = new Tenant(tenantId, name);
        return tenantRepository.save(tenant);
    }

    /**
     * Get tenant by tenant ID.
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> getTenantByTenantId(String tenantId) {
        return tenantRepository.findByTenantId(tenantId);
    }

    /**
     * Get all tenants.
     */
    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Update tenant name.
     */
    public Tenant updateTenantName(String tenantId, String newName) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.setName(newName);
        return tenantRepository.save(tenant);
    }

    /**
     * Delete tenant by tenant ID.
     */
    public void deleteTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenantRepository.delete(tenant);
    }
}
