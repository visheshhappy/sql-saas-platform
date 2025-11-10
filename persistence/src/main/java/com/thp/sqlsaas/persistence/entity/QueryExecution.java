package com.thp.sqlsaas.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Query Execution entity - tracks the state of query executions.
 */
@Entity
@Table(name = "query_executions")
public class QueryExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "trace_id", unique = true, nullable = false)
    private String traceId;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;
    
    @Column(name = "connector_type")
    private String connectorType;
    
    @Column(name = "resource")
    private String resource;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private QueryState state;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "rows_returned")
    private Integer rowsReturned;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "freshness_ms")
    private Long freshnessMs;
    
    @Column(name = "cache_hit")
    private Boolean cacheHit;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Query execution states.
     */
    public enum QueryState {
        PENDING,        // Query submitted
        VALIDATING,     // Checking entitlements
        RATE_LIMITED,   // Rate limit exceeded
        EXECUTING,      // Executing on connector
        COMPLETED,      // Successfully completed
        FAILED,         // Failed with error
        CANCELLED       // Cancelled by user
    }
    
    // Constructors
    public QueryExecution() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.state = QueryState.PENDING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
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
    
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
    
    public String getConnectorType() {
        return connectorType;
    }
    
    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }
    
    public String getResource() {
        return resource;
    }
    
    public void setResource(String resource) {
        this.resource = resource;
    }
    
    public QueryState getState() {
        return state;
    }
    
    public void setState(QueryState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRowsReturned() {
        return rowsReturned;
    }
    
    public void setRowsReturned(Integer rowsReturned) {
        this.rowsReturned = rowsReturned;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Long getFreshnessMs() {
        return freshnessMs;
    }
    
    public void setFreshnessMs(Long freshnessMs) {
        this.freshnessMs = freshnessMs;
    }
    
    public Boolean getCacheHit() {
        return cacheHit;
    }
    
    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
