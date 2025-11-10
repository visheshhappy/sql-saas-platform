package com.thp.sqlsaas.server.model;

import java.util.List;
import java.util.Map;

/**
 * Result of query execution.
 * Contains data, metadata about freshness, rate limits, and errors.
 */
public class QueryExecutionResult {
    private String status;  // SUCCESS, ERROR, RATE_LIMIT_EXCEEDED
    private List<Map<String, Object>> rows;
    private List<String> columns;
    private String nextPageToken;
    private Long freshnessMs;
    private String rateLimitStatus;
    private Integer remainingRequests;
    private Long retryAfterSeconds;
    private String errorCode;
    private String errorMessage;
    private String traceId;
    private Long executionTimeMs;
    
    public QueryExecutionResult() {
    }
    
    public static QueryExecutionResult success(
            List<Map<String, Object>> rows,
            String nextPageToken,
            Long freshnessMs,
            String rateLimitStatus) {
        
        QueryExecutionResult result = new QueryExecutionResult();
        result.status = "SUCCESS";
        result.rows = rows;
        result.nextPageToken = nextPageToken;
        result.freshnessMs = freshnessMs;
        result.rateLimitStatus = rateLimitStatus;
        
        // Extract columns from first row
        if (rows != null && !rows.isEmpty()) {
            result.columns = List.copyOf(rows.get(0).keySet());
        }
        
        return result;
    }
    
    public static QueryExecutionResult error(
            String errorCode,
            String errorMessage,
            Long executionTimeMs) {
        
        QueryExecutionResult result = new QueryExecutionResult();
        result.status = "ERROR";
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        result.executionTimeMs = executionTimeMs;
        return result;
    }
    
    public static QueryExecutionResult rateLimitExceeded(
            Long retryAfterSeconds,
            String message) {
        
        QueryExecutionResult result = new QueryExecutionResult();
        result.status = "RATE_LIMIT_EXCEEDED";
        result.errorCode = "RATE_LIMIT_EXHAUSTED";
        result.errorMessage = message;
        result.retryAfterSeconds = retryAfterSeconds;
        return result;
    }
    
    // Getters and setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<Map<String, Object>> getRows() {
        return rows;
    }
    
    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    
    public String getNextPageToken() {
        return nextPageToken;
    }
    
    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
    
    public Long getFreshnessMs() {
        return freshnessMs;
    }
    
    public void setFreshnessMs(Long freshnessMs) {
        this.freshnessMs = freshnessMs;
    }
    
    public String getRateLimitStatus() {
        return rateLimitStatus;
    }
    
    public void setRateLimitStatus(String rateLimitStatus) {
        this.rateLimitStatus = rateLimitStatus;
    }
    
    public Integer getRemainingRequests() {
        return remainingRequests;
    }
    
    public void setRemainingRequests(Integer remainingRequests) {
        this.remainingRequests = remainingRequests;
    }
    
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public void setRetryAfterSeconds(Long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
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
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
