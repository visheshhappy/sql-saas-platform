package com.thp.sqlsaas.server.controller;

import com.thp.sqlsaas.persistence.entity.Tenant;
import com.thp.sqlsaas.persistence.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tenant management.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Get all tenants.
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    /**
     * Get tenant by ID.
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenantByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new tenant.
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.tenantId(), request.name());
        return ResponseEntity.ok(tenant);
    }

    /**
     * Update tenant name.
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantRequest request) {
        Tenant tenant = tenantService.updateTenantName(tenantId, request.name());
        return ResponseEntity.ok(tenant);
    }

    /**
     * Delete tenant.
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    // Request DTOs
    public record CreateTenantRequest(String tenantId, String name) {}
    public record UpdateTenantRequest(String name) {}
}
