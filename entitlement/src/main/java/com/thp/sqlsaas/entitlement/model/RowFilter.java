package com.thp.sqlsaas.entitlement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a row-level filter to be applied.
 * Can be converted to SQL predicate or applied in-memory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowFilter {
    
    private String columnName;
    private FilterOperator operator;
    private Object value;
    private String policyId;
    
    public enum FilterOperator {
        EQUALS("="),
        NOT_EQUALS("!="),
        IN("IN"),
        NOT_IN("NOT IN"),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        CONTAINS("LIKE");
        
        private final String sql;
        
        FilterOperator(String sql) {
            this.sql = sql;
        }
        
        public String getSql() {
            return sql;
        }
    }
    
    /**
     * Convert to SQL WHERE clause fragment
     */
    public String toSqlPredicate() {
        StringBuilder sb = new StringBuilder();
        sb.append(columnName).append(" ").append(operator.getSql()).append(" ");
        
        if (operator == FilterOperator.IN || operator == FilterOperator.NOT_IN) {
            sb.append("(").append(formatValue()).append(")");
        } else if (operator == FilterOperator.CONTAINS) {
            sb.append("'%").append(value).append("%'");
        } else {
            sb.append(formatValue());
        }
        
        return sb.toString();
    }
    
    /**
     * Convert to Connector Predicate
     */
    public com.thp.sqlsaas.connector.Connector.Predicate toPredicate() {
        String op = switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
            case GREATER_THAN -> ">";
            case LESS_THAN -> "<";
            case CONTAINS -> "LIKE";
        };
        
        return new com.thp.sqlsaas.connector.Connector.Predicate(columnName, op, value);
    }
    
    private String formatValue() {
        if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object v : (Iterable<?>) value) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(formatSingleValue(v));
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }
    
    private String formatSingleValue(Object v) {
        if (v instanceof String) {
            return "'" + v + "'";
        }
        return String.valueOf(v);
    }
}
