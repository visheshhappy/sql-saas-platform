package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains all information needed to make entitlement decisions.
 * Extracted from authentication token and enriched with source permissions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementContext {
    
    private String userId;
    private String tenantId;
    private String email;
    
    // Resource being accessed (table name)
    private String resource;
    
    // Columns requested in the query
    private Set<String> requestedColumns;
    
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    
    @Builder.Default
    private Set<String> scopes = new HashSet<>();
    
    // Permissions from source systems (extracted from OAuth tokens)
    @Builder.Default
    private Map<String, SourcePermissions> sourcePermissions = new HashMap<>();
    
    // Tenant-specific policies
    private TenantPolicy tenantPolicy;
    
    // User attributes for policy evaluation (department, region, etc.)
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    public void addSourcePermissions(String sourceId, SourcePermissions permissions) {
        this.sourcePermissions.put(sourceId, permissions);
    }
    
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
}
