package com.thp.sqlsaas.model;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a parsed SQL query that can be sent to the connector module. Contains the essential
 * information extracted from SQL parsing.
 */
@EqualsAndHashCode
@ToString
public class SqlQueryRequest {

    private String tableName;
    private List<Filter> filters;

    public SqlQueryRequest() {
        this.filters = new ArrayList<>();
    }

    public SqlQueryRequest(String tableName, List<Filter> filters) {
        this.tableName = tableName;
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public void addFilter(Filter filter) {
        if (this.filters == null) {
            this.filters = new ArrayList<>();
        }
        this.filters.add(filter);
    }
}
