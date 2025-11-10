package com.thp.sqlsaas.persistence.service;

import com.thp.sqlsaas.persistence.entity.QueryExecution;
import com.thp.sqlsaas.persistence.entity.QueryExecution.QueryState;
import com.thp.sqlsaas.persistence.repository.QueryExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing query execution state.
 * Acts as a state machine for tracking query lifecycle.
 */
@Service
public class QueryExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionService.class);
    
    private final QueryExecutionRepository repository;
    
    public QueryExecutionService(QueryExecutionRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Create a new query execution record.
     */
    @Transactional
    public QueryExecution createExecution(
            String traceId,
            String tenantId,
            String userId,
            String sqlQuery,
            String connectorType,
            String resource) {
        
        QueryExecution execution = new QueryExecution();
        execution.setTraceId(traceId);
        execution.setTenantId(tenantId);
        execution.setUserId(userId);
        execution.setSqlQuery(sqlQuery);
        execution.setConnectorType(connectorType);
        execution.setResource(resource);
        execution.setState(QueryState.PENDING);
        execution.setCacheHit(false);
        
        QueryExecution saved = repository.save(execution);
        logger.info("Created query execution record - traceId: {}, tenant: {}, user: {}",
                   traceId, tenantId, userId);
        return saved;
    }
    
    /**
     * Update state of query execution.
     */
    @Transactional
    public void updateState(String traceId, QueryState newState) {
        Optional<QueryExecution> opt = repository.findByTraceId(traceId);
        if (opt.isPresent()) {
            QueryExecution execution = opt.get();
            execution.setState(newState);
            repository.save(execution);
            logger.debug("Updated query execution state - traceId: {}, state: {}", 
                        traceId, newState);
        } else {
            logger.warn("Query execution not found for traceId: {}", traceId);
        }
    }
    
    /**
     * Mark execution as completed with results.
     */
    @Transactional
    public void completeExecution(
            String traceId,
            String status,
            Integer rowsReturned,
            Long executionTimeMs,
            Long freshnessMs,
            Boolean cacheHit) {
        
        Optional<QueryExecution> opt = repository.findByTraceId(traceId);
        if (opt.isPresent()) {
            QueryExecution execution = opt.get();
            execution.setState(QueryState.COMPLETED);
            execution.setStatus(status);
            execution.setRowsReturned(rowsReturned);
            execution.setExecutionTimeMs(executionTimeMs);
            execution.setFreshnessMs(freshnessMs);
            execution.setCacheHit(cacheHit);
            execution.setCompletedAt(Instant.now());
            
            repository.save(execution);
            logger.info("Completed query execution - traceId: {}, status: {}, rows: {}, time: {}ms",
                       traceId, status, rowsReturned, executionTimeMs);
        } else {
            logger.warn("Query execution not found for traceId: {}", traceId);
        }
    }
    
    /**
     * Mark execution as failed with error details.
     */
    @Transactional
    public void failExecution(
            String traceId,
            String errorCode,
            String errorMessage,
            Long executionTimeMs) {
        
        Optional<QueryExecution> opt = repository.findByTraceId(traceId);
        if (opt.isPresent()) {
            QueryExecution execution = opt.get();
            execution.setState(QueryState.FAILED);
            execution.setStatus("ERROR");
            execution.setErrorCode(errorCode);
            execution.setErrorMessage(errorMessage);
            execution.setExecutionTimeMs(executionTimeMs);
            execution.setCompletedAt(Instant.now());
            
            repository.save(execution);
            logger.info("Failed query execution - traceId: {}, errorCode: {}", 
                       traceId, errorCode);
        } else {
            logger.warn("Query execution not found for traceId: {}", traceId);
        }
    }
    
    /**
     * Get query execution by trace ID.
     */
    public Optional<QueryExecution> getExecution(String traceId) {
        return repository.findByTraceId(traceId);
    }
    
    /**
     * Get recent executions for a tenant.
     */
    public List<QueryExecution> getRecentExecutions(String tenantId, int limit) {
        List<QueryExecution> executions = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return executions.stream().limit(limit).toList();
    }
    
    /**
     * Get executions by state.
     */
    public List<QueryExecution> getExecutionsByState(QueryState state) {
        return repository.findByState(state);
    }
}
