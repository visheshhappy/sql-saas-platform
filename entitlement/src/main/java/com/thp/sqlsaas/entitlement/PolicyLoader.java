package com.thp.sqlsaas.entitlement;

import com.thp.sqlsaas.entitlement.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Loads and converts database policy entities to domain model.
 * This adapter sits between persistence layer and entitlement service.
 */
@Component
public class PolicyLoader {
    
    /**
     * Convert database policy to domain Policy model.
     */
    public Policy convertToPolicy(com.thp.sqlsaas.persistence.entity.EntitlementPolicy entity) {
        Policy.PolicyBuilder builder = Policy.builder()
                .policyId(entity.getPolicyId())
                .type(mapPolicyType(entity.getPolicyType()))
                .action(mapPolicyAction(entity.getAction()))
                .sourcePattern(entity.getSourcePattern())
                .tablePattern(entity.getTablePattern())
                .condition(entity.getCondition());
        
        // Parse policy config based on type
        Map<String, Object> config = entity.getPolicyConfig();
        
        switch (entity.getPolicyType()) {
            case "RLS" -> builder.rowFilter(parseRowFilter(config, entity.getPolicyId()));
            case "CLS" -> builder.allowedColumns(parseAllowedColumns(config));
            case "MASK" -> {
                builder.columnToMask((String) config.get("column"));
                builder.columnMask(parseColumnMask(config, entity.getPolicyId()));
            }
        }
        
        return builder.build();
    }
    
    /**
     * Convert multiple entities to TenantPolicy.
     */
    public TenantPolicy convertToTenantPolicy(
            String tenantId,
            List<com.thp.sqlsaas.persistence.entity.EntitlementPolicy> entities) {
        
        List<Policy> policies = entities.stream()
                .map(this::convertToPolicy)
                .toList();
        
        return TenantPolicy.builder()
                .tenantId(tenantId)
                .policies(policies)
                .build();
    }
    
    private Policy.PolicyType mapPolicyType(String type) {
        return switch (type) {
            case "RLS" -> Policy.PolicyType.RLS;
            case "CLS" -> Policy.PolicyType.CLS;
            case "MASK" -> Policy.PolicyType.MASK;
            case "TABLE_ACCESS" -> Policy.PolicyType.TABLE_ACCESS;
            default -> throw new IllegalArgumentException("Unknown policy type: " + type);
        };
    }
    
    private Policy.PolicyAction mapPolicyAction(String action) {
        return switch (action) {
            case "ALLOW" -> Policy.PolicyAction.ALLOW;
            case "DENY" -> Policy.PolicyAction.DENY;
            case "FILTER" -> Policy.PolicyAction.FILTER;
            case "MASK" -> Policy.PolicyAction.MASK;
            default -> throw new IllegalArgumentException("Unknown policy action: " + action);
        };
    }
    
    private RowFilter parseRowFilter(Map<String, Object> config, String policyId) {
        if (config == null || !config.containsKey("column")) {
            return null;
        }
        
        String column = (String) config.get("column");
        String operator = (String) config.get("operator");
        Object value = config.get("value");
        
        return RowFilter.builder()
                .columnName(column)
                .operator(mapOperator(operator))
                .value(value)
                .policyId(policyId)
                .build();
    }
    
    private RowFilter.FilterOperator mapOperator(String op) {
        return switch (op.toUpperCase()) {
            case "=" -> RowFilter.FilterOperator.EQUALS;
            case "!=" -> RowFilter.FilterOperator.NOT_EQUALS;
            case "IN" -> RowFilter.FilterOperator.IN;
            case "NOT IN" -> RowFilter.FilterOperator.NOT_IN;
            case ">" -> RowFilter.FilterOperator.GREATER_THAN;
            case "<" -> RowFilter.FilterOperator.LESS_THAN;
            case "LIKE" -> RowFilter.FilterOperator.CONTAINS;
            default -> RowFilter.FilterOperator.EQUALS;
        };
    }
    
    private Set<String> parseAllowedColumns(Map<String, Object> config) {
        if (config == null) {
            return new HashSet<>();
        }
        
        // For CLS DENY, we have "denied_columns"
        if (config.containsKey("denied_columns")) {
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("denied_columns");
            // Note: This returns denied columns, which will be removed from allowed set
            return new HashSet<>(columns);
        }
        
        // For CLS ALLOW, we have "allowed_columns"
        if (config.containsKey("allowed_columns")) {
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("allowed_columns");
            return new HashSet<>(columns);
        }
        
        return new HashSet<>();
    }
    
    private ColumnMask parseColumnMask(Map<String, Object> config, String policyId) {
        if (config == null || !config.containsKey("mask_type")) {
            return null;
        }
        
        String maskTypeStr = (String) config.get("mask_type");
        ColumnMask.MaskType maskType = mapMaskType(maskTypeStr);
        
        return ColumnMask.builder()
                .maskType(maskType)
                .policyId(policyId)
                .build();
    }
    
    private ColumnMask.MaskType mapMaskType(String type) {
        return switch (type.toUpperCase()) {
            case "FULL" -> ColumnMask.MaskType.FULL;
            case "PARTIAL" -> ColumnMask.MaskType.PARTIAL;
            case "HASH" -> ColumnMask.MaskType.HASH;
            case "REDACT" -> ColumnMask.MaskType.REDACT;
            case "NULL" -> ColumnMask.MaskType.NULL;
            default -> ColumnMask.MaskType.FULL;
        };
    }
}
