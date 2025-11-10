package com.thp.sqlsaas.model;

import java.util.Objects;

/**
 * Represents a filter condition from a SQL WHERE clause.
 */
public class Filter {
    
    private String columnName;
    private FilterOperator operator;
    private Object value;
    
    public Filter() {
    }
    
    public Filter(String columnName, FilterOperator operator, Object value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public FilterOperator getOperator() {
        return operator;
    }
    
    public void setOperator(FilterOperator operator) {
        this.operator = operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return Objects.equals(columnName, filter.columnName) && 
               operator == filter.operator && 
               Objects.equals(value, filter.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(columnName, operator, value);
    }
    
    @Override
    public String toString() {
        return "Filter{" +
                "columnName='" + columnName + '\'' +
                ", operator=" + operator +
                ", value=" + value +
                '}';
    }
}
