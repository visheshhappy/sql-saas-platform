package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of policies for a tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPolicy {
    
    private String tenantId;
    
    @Builder.Default
    private List<Policy> policies = new ArrayList<>();
    
    public List<Policy> getPoliciesForSource(String sourceId, String tableName) {
        return policies.stream()
                .filter(p -> p.matches(sourceId, tableName))
                .toList();
    }
}
