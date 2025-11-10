package com.thp.sqlsaas.entitlement;

import com.thp.sqlsaas.entitlement.model.*;
import com.thp.sqlsaas.persistence.entity.EntitlementPolicy;
import com.thp.sqlsaas.persistence.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Main service for entitlement enforcement.
 * Evaluates policies and produces entitlement decisions.
 */
@Service
public class EntitlementService {
    
    private static final Logger logger = LoggerFactory.getLogger(EntitlementService.class);
    
    @Autowired
    private PolicyService policyService;
    
    @Autowired
    private PolicyLoader policyLoader;
    
    /**
     * Evaluate access for a query execution context.
     * Simplified version that delegates to authorizeQuery.
     */
    public EntitlementDecision evaluateAccess(EntitlementContext context) {
        // For now, we'll use a simple authorization that allows everything
        // In production, this would delegate to authorizeQuery with proper table/column info
        
        logger.debug("Evaluating access for: tenant={}, user={}, resource={}", 
                context.getTenantId(), context.getUserId(), context.getResource());
        
        // Extract table from resource (e.g., "issues" -> "github_issues")
        String tableName = context.getResource();
        String sourceId = "default"; // Would be determined from context in production
        
        Set<String> requestedColumns = context.getRequestedColumns() != null 
            ? context.getRequestedColumns() 
            : Set.of("*");
        
        return authorizeQuery(context, sourceId, tableName, requestedColumns);
    }
    
    /**
     * Authorize query execution
     */
    public EntitlementDecision authorizeQuery(
            EntitlementContext context,
            String sourceId,
            String tableName,
            Set<String> requestedColumns) {
        
        logger.debug("Authorizing query: tenant={}, source={}, table={}, columns={}", 
                context.getTenantId(), sourceId, tableName, requestedColumns);
        
        // 1. Check if user has any access at all
        if (context.getUserId() == null) {
            return EntitlementDecision.deny("User not authenticated");
        }
        
        // 2. Load policies from database
        List<Policy> policies = loadPolicies(context, sourceId, tableName);
        
        logger.debug("Found {} applicable policies", policies.size());
        
        // 3. Check for explicit DENY policies first
        for (Policy policy : policies) {
            if (policy.getType() == Policy.PolicyType.TABLE_ACCESS &&
                policy.getAction() == Policy.PolicyAction.DENY &&
                policy.evaluate(context)) {
                
                logger.info("Access denied by policy: {}", policy.getPolicyId());
                return EntitlementDecision.deny(
                    "Access denied by policy: " + policy.getPolicyId()
                );
            }
        }
        
        // 4. Check for explicit ALLOW policies (e.g., admin bypass)
        for (Policy policy : policies) {
            if (policy.getType() == Policy.PolicyType.TABLE_ACCESS &&
                policy.getAction() == Policy.PolicyAction.ALLOW &&
                policy.evaluate(context)) {
                
                logger.debug("Access allowed by policy: {}", policy.getPolicyId());
                // Admin or special role - allow everything
                EntitlementDecision decision = EntitlementDecision.allow();
                decision.setAllowedColumns(new HashSet<>(requestedColumns));
                decision.getAppliedPolicies().add(policy.getPolicyId());
                return decision;
            }
        }
        
        // 5. Get source permissions
        SourcePermissions sourcePerms = context.getSourcePermissions().get(sourceId);
        if (sourcePerms == null) {
            // No source permissions - create default that allows requested table
            logger.debug("No source permissions found, creating default permissions");
            sourcePerms = SourcePermissions.builder()
                    .readableTables(Set.of(tableName))
                    .build();
            
            // Add all requested columns
            Map<String, Set<String>> tableColumns = new HashMap<>();
            tableColumns.put(tableName, new HashSet<>(requestedColumns));
            sourcePerms.setTableColumns(tableColumns);
        }
        
        // 6. Check table access
        if (!sourcePerms.canReadTable(tableName)) {
            return EntitlementDecision.deny(
                "User cannot access table: " + tableName + " in source: " + sourceId
            );
        }
        
        // 7. Build decision
        EntitlementDecision decision = EntitlementDecision.allow();
        
        // 8. Compute allowed columns (CLS)
        Set<String> allowedColumns = computeAllowedColumns(
            context, sourcePerms, policies, tableName, requestedColumns
        );
        decision.setAllowedColumns(allowedColumns);
        
        // 9. Compute row filters (RLS)
        List<RowFilter> rowFilters = computeRowFilters(
            context, sourcePerms, policies, tableName
        );
        decision.setRowFilters(rowFilters);
        
        // 10. Compute column masks
        Map<String, ColumnMask> columnMasks = computeColumnMasks(
            context, policies, tableName, requestedColumns
        );
        decision.setColumnMasks(columnMasks);
        
        // 11. Track applied policies
        decision.setAppliedPolicies(
            policies.stream()
                .filter(p -> p.evaluate(context))
                .map(Policy::getPolicyId)
                .toList()
        );
        
        logger.info("Authorization complete: allowed={}, filters={}, masks={}, policies={}", 
                decision.isAllowed(), 
                decision.getRowFilters().size(),
                decision.getColumnMasks().size(),
                decision.getAppliedPolicies().size());
        
        return decision;
    }
    
    private List<Policy> loadPolicies(
            EntitlementContext context,
            String sourceId,
            String tableName) {
        
        // Load from database via PolicyService
        List<EntitlementPolicy> entities = policyService.getApplicablePolicies(
                context.getTenantId(), sourceId, tableName);
        
        // Convert to domain models
        List<Policy> policies = entities.stream()
                .map(policyLoader::convertToPolicy)
                .sorted(Comparator.comparing((Policy p) -> 
                    entities.stream()
                        .filter(e -> e.getPolicyId().equals(p.getPolicyId()))
                        .findFirst()
                        .map(com.thp.sqlsaas.persistence.entity.EntitlementPolicy::getPriority)
                        .orElse(0))
                    .reversed()) // Higher priority first
                .toList();
        
        return policies;
    }
    
    private Set<String> computeAllowedColumns(
            EntitlementContext context,
            SourcePermissions sourcePerms,
            List<Policy> policies,
            String tableName,
            Set<String> requestedColumns) {
        
        Set<String> allowed = new HashSet<>(requestedColumns);
        
        // Start with source permissions
        Set<String> sourceColumns = sourcePerms.getTableColumns().get(tableName);
        if (sourceColumns != null && !sourceColumns.isEmpty()) {
            allowed.retainAll(sourceColumns);
        }
        
        // Apply CLS policies
        for (Policy policy : policies) {
            if (policy.getType() == Policy.PolicyType.CLS &&
                policy.evaluate(context)) {
                
                if (policy.getAction() == Policy.PolicyAction.DENY) {
                    // Remove denied columns
                    allowed.removeAll(policy.getAllowedColumns());
                    logger.debug("Policy {} removed columns: {}", 
                            policy.getPolicyId(), policy.getAllowedColumns());
                } else if (policy.getAction() == Policy.PolicyAction.ALLOW) {
                    // Only keep allowed columns
                    allowed.retainAll(policy.getAllowedColumns());
                    logger.debug("Policy {} restricted to columns: {}", 
                            policy.getPolicyId(), policy.getAllowedColumns());
                }
            }
        }
        
        return allowed;
    }
    
    private List<RowFilter> computeRowFilters(
            EntitlementContext context,
            SourcePermissions sourcePerms,
            List<Policy> policies,
            String tableName) {
        
        List<RowFilter> filters = new ArrayList<>();
        
        // Add source-native filters
        String nativeFilter = sourcePerms.getNativeRowFilters().get(tableName);
        if (nativeFilter != null) {
            // In a real implementation, parse and add native filter
            logger.debug("Native filter found but not parsed: {}", nativeFilter);
        }
        
        // Add policy-based filters
        for (Policy policy : policies) {
            if (policy.getType() == Policy.PolicyType.RLS &&
                policy.evaluate(context)) {
                
                RowFilter filter = policy.getRowFilter();
                if (filter != null) {
                    // Substitute user context values
                    filter = substituteContextValues(filter, context);
                    filters.add(filter);
                    logger.debug("Added RLS filter from policy {}: {} {} {}", 
                            policy.getPolicyId(),
                            filter.getColumnName(),
                            filter.getOperator(),
                            filter.getValue());
                }
            }
        }
        
        return filters;
    }
    
    private RowFilter substituteContextValues(RowFilter filter, EntitlementContext context) {
        // Replace placeholders like ${user.region} with actual values
        Object value = filter.getValue();
        
        if (value instanceof String) {
            String strValue = (String) value;
            
            if (strValue.startsWith("${") && strValue.endsWith("}")) {
                String placeholder = strValue.substring(2, strValue.length() - 1);
                
                Object substitutedValue = extractContextValue(placeholder, context);
                if (substitutedValue != null) {
                    filter.setValue(substitutedValue);
                    logger.debug("Substituted {} with {}", strValue, substitutedValue);
                }
            }
        }
        
        return filter;
    }
    
    private Object extractContextValue(String placeholder, EntitlementContext context) {
        // Handle user.id, user.email, user.department, user.region, etc.
        if (placeholder.equals("user.id")) {
            return context.getUserId();
        } else if (placeholder.equals("user.email")) {
            return context.getEmail();
        } else if (placeholder.startsWith("user.")) {
            String attrName = placeholder.substring(5); // Remove "user."
            return context.getAttributes().get(attrName);
        }
        
        return null;
    }
    
    private Map<String, ColumnMask> computeColumnMasks(
            EntitlementContext context,
            List<Policy> policies,
            String tableName,
            Set<String> requestedColumns) {
        
        Map<String, ColumnMask> masks = new HashMap<>();
        
        for (Policy policy : policies) {
            if (policy.getType() == Policy.PolicyType.MASK &&
                policy.evaluate(context) &&
                requestedColumns.contains(policy.getColumnToMask())) {
                
                masks.put(policy.getColumnToMask(), policy.getColumnMask());
                logger.debug("Added column mask from policy {} for column {}", 
                        policy.getPolicyId(), policy.getColumnToMask());
            }
        }
        
        return masks;
    }
    
    /**
     * Apply row filters to a result set (post-processing if needed)
     */
    public List<Map<String, Object>> applyRowFilters(
            List<Map<String, Object>> rows,
            List<RowFilter> filters) {
        
        if (filters == null || filters.isEmpty()) {
            return rows;
        }
        
        return rows.stream()
                .filter(row -> matchesAllFilters(row, filters))
                .toList();
    }
    
    private boolean matchesAllFilters(Map<String, Object> row, List<RowFilter> filters) {
        for (RowFilter filter : filters) {
            if (!matchesFilter(row, filter)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean matchesFilter(Map<String, Object> row, RowFilter filter) {
        Object value = row.get(filter.getColumnName());
        Object filterValue = filter.getValue();
        
        if (value == null) {
            return false;
        }
        
        return switch (filter.getOperator()) {
            case EQUALS -> value.equals(filterValue);
            case NOT_EQUALS -> !value.equals(filterValue);
            case IN -> filterValue instanceof Collection && 
                      ((Collection<?>) filterValue).contains(value);
            case NOT_IN -> !(filterValue instanceof Collection && 
                           ((Collection<?>) filterValue).contains(value));
            case GREATER_THAN -> compareValues(value, filterValue) > 0;
            case LESS_THAN -> compareValues(value, filterValue) < 0;
            case CONTAINS -> value.toString().contains(filterValue.toString());
        };
    }
    
    @SuppressWarnings("unchecked")
    private int compareValues(Object v1, Object v2) {
        if (v1 instanceof Comparable && v2 instanceof Comparable) {
            return ((Comparable) v1).compareTo(v2);
        }
        return v1.toString().compareTo(v2.toString());
    }
    
    /**
     * Apply column masks to a result set
     */
    public List<Map<String, Object>> applyColumnMasks(
            List<Map<String, Object>> rows,
            Map<String, ColumnMask> masks) {
        
        if (masks == null || masks.isEmpty()) {
            return rows;
        }
        
        return rows.stream()
                .map(row -> applyMasksToRow(row, masks))
                .toList();
    }
    
    private Map<String, Object> applyMasksToRow(
            Map<String, Object> row, 
            Map<String, ColumnMask> masks) {
        
        Map<String, Object> maskedRow = new HashMap<>(row);
        
        for (Map.Entry<String, ColumnMask> entry : masks.entrySet()) {
            String column = entry.getKey();
            ColumnMask mask = entry.getValue();
            
            if (maskedRow.containsKey(column)) {
                Object originalValue = maskedRow.get(column);
                Object maskedValue = mask.mask(originalValue);
                maskedRow.put(column, maskedValue);
            }
        }
        
        return maskedRow;
    }
}
