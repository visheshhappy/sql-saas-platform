package com.thp.sqlsaas.server.model;

import com.thp.sqlsaas.connector.Connector;
import com.thp.sqlsaas.connector.ConnectorType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Query execution plan created by the planner.
 * Contains all information needed to execute a query.
 */
public class QueryPlan {
    private String tenantId;
    private String userId;
    private Set<String> userRoles;
    
    // Connector information
    private ConnectorType connectorType;
    private Map<String, String> connectorConfig;
    
    // Query details
    private String sqlQuery;  // Original SQL query
    private String resource;  // e.g., "issues", "pulls"
    private List<String> requestedColumns;
    private List<Connector.Predicate> predicates;
    private Integer limit;
    private Long maxStalenessMs;
    
    // Metadata
    private String traceId;
    
    public QueryPlan() {
    }
    
    public QueryPlan(String tenantId, String userId, Set<String> userRoles,
                     ConnectorType connectorType, Map<String, String> connectorConfig,
                     String sqlQuery, String resource, List<String> requestedColumns,
                     List<Connector.Predicate> predicates, Integer limit,
                     Long maxStalenessMs, String traceId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.userRoles = userRoles;
        this.connectorType = connectorType;
        this.connectorConfig = connectorConfig;
        this.sqlQuery = sqlQuery;
        this.resource = resource;
        this.requestedColumns = requestedColumns;
        this.predicates = predicates;
        this.limit = limit;
        this.maxStalenessMs = maxStalenessMs;
        this.traceId = traceId;
    }
    
    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Set<String> getUserRoles() {
        return userRoles;
    }
    
    public void setUserRoles(Set<String> userRoles) {
        this.userRoles = userRoles;
    }
    
    public ConnectorType getConnectorType() {
        return connectorType;
    }
    
    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }
    
    public Map<String, String> getConnectorConfig() {
        return connectorConfig;
    }
    
    public void setConnectorConfig(Map<String, String> connectorConfig) {
        this.connectorConfig = connectorConfig;
    }
    
    public String getResource() {
        return resource;
    }
    
    public void setResource(String resource) {
        this.resource = resource;
    }
    
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
    
    public List<String> getRequestedColumns() {
        return requestedColumns;
    }
    
    public void setRequestedColumns(List<String> requestedColumns) {
        this.requestedColumns = requestedColumns;
    }
    
    public List<Connector.Predicate> getPredicates() {
        return predicates;
    }
    
    public void setPredicates(List<Connector.Predicate> predicates) {
        this.predicates = predicates;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Long getMaxStalenessMs() {
        return maxStalenessMs;
    }
    
    public void setMaxStalenessMs(Long maxStalenessMs) {
        this.maxStalenessMs = maxStalenessMs;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
