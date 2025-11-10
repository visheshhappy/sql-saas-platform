package com.thp.sqlsaas.model;

/**
 * Enum representing SQL comparison operators.
 */
public enum FilterOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    BETWEEN("BETWEEN");
    
    private final String symbol;
    
    FilterOperator(String symbol) {
        this.symbol = symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Parse operator from SQL string representation.
     */
    public static FilterOperator fromString(String operatorStr) {
        if (operatorStr == null) {
            return null;
        }
        
        String normalized = operatorStr.trim().toUpperCase();
        
        switch (normalized) {
            case "=":
                return EQUALS;
            case "!=":
            case "<>":
                return NOT_EQUALS;
            case ">":
                return GREATER_THAN;
            case ">=":
                return GREATER_THAN_OR_EQUAL;
            case "<":
                return LESS_THAN;
            case "<=":
                return LESS_THAN_OR_EQUAL;
            case "LIKE":
                return LIKE;
            case "NOT LIKE":
                return NOT_LIKE;
            case "IN":
                return IN;
            case "NOT IN":
                return NOT_IN;
            case "IS NULL":
                return IS_NULL;
            case "IS NOT NULL":
                return IS_NOT_NULL;
            case "BETWEEN":
                return BETWEEN;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operatorStr);
        }
    }
    
    @Override
    public String toString() {
        return symbol;
    }
}
