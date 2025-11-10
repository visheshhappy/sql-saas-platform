package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Individual policy rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    
    private String policyId;
    private PolicyType type;
    private PolicyAction action;
    
    // Scope
    private String sourcePattern;   // "github.*", "salesforce", "*"
    private String tablePattern;    // "issues", "accounts", "*"
    
    // Conditions
    private String condition;       // Simple expression: "user.role == 'ADMIN'"
    
    // Actions
    @Builder.Default
    private Set<String> allowedColumns = new HashSet<>();
    private RowFilter rowFilter;
    private ColumnMask columnMask;
    private String columnToMask;
    
    public enum PolicyType {
        TABLE_ACCESS,   // Allow/deny table access
        RLS,            // Row-level security
        CLS,            // Column-level security
        MASK            // Column masking
    }
    
    public enum PolicyAction {
        ALLOW,
        DENY,
        FILTER,
        MASK
    }
    
    /**
     * Check if policy applies to given source and table
     */
    public boolean matches(String sourceId, String tableName) {
        boolean sourceMatch = matchesPattern(sourceId, sourcePattern);
        boolean tableMatch = matchesPattern(tableName, tablePattern);
        return sourceMatch && tableMatch;
    }
    
    private boolean matchesPattern(String value, String pattern) {
        if (pattern == null || pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return value.startsWith(prefix);
        }
        return pattern.equals(value);
    }
    
    /**
     * Evaluate condition against context
     */
    public boolean evaluate(EntitlementContext context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // Simple expression evaluator
        // Format: "user.role == 'ADMIN'" or "user.department == 'SALES'"
        try {
            // Handle != operator
            if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                if (parts.length != 2) {
                    return false;
                }
                
                String left = parts[0].trim();
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                
                return !evaluateEquals(left, right, context);
            }
            
            // Handle == operator
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length != 2) {
                    return false;
                }
                
                String left = parts[0].trim();
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                
                return evaluateEquals(left, right, context);
            }
            
            // Handle = operator
            if (condition.contains("=")) {
                String[] parts = condition.split("=");
                if (parts.length != 2) {
                    return false;
                }
                
                String left = parts[0].trim();
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                
                return evaluateEquals(left, right, context);
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluateEquals(String left, String right, EntitlementContext context) {
        if (left.startsWith("user.role")) {
            return context.hasRole(right);
        } else if (left.startsWith("user.department")) {
            Object dept = context.getAttributes().get("department");
            return dept != null && dept.toString().equals(right);
        } else if (left.startsWith("user.region")) {
            Object region = context.getAttributes().get("region");
            return region != null && region.toString().equals(right);
        } else if (left.startsWith("user.type")) {
            Object type = context.getAttributes().get("type");
            return type != null && type.toString().equals(right);
        }
        
        return false;
    }
}
