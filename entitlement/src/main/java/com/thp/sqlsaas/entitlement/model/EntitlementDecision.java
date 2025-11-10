package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of entitlement evaluation.
 * Contains everything needed to enforce security on a query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementDecision {
    
    private boolean allowed;
    private String denialReason;
    
    // Columns that can be returned (CLS)
    @Builder.Default
    private Set<String> allowedColumns = new HashSet<>();
    
    // Filters to apply to results (RLS)
    @Builder.Default
    private List<RowFilter> rowFilters = new ArrayList<>();
    
    // Column masking rules (partial visibility)
    @Builder.Default
    private Map<String, ColumnMask> columnMasks = new HashMap<>();
    
    // Applied policies (for audit)
    @Builder.Default
    private List<String> appliedPolicies = new ArrayList<>();
    
    public static EntitlementDecision deny(String reason) {
        return EntitlementDecision.builder()
                .allowed(false)
                .denialReason(reason)
                .build();
    }
    
    public static EntitlementDecision allow() {
        return EntitlementDecision.builder()
                .allowed(true)
                .build();
    }
}
