package com.thp.sqlsaas.server.orchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thp.sqlsaas.connector.Connector;
import com.thp.sqlsaas.connector.ConnectorFactory;
import com.thp.sqlsaas.entitlement.EntitlementService;
import com.thp.sqlsaas.entitlement.model.ColumnMask;
import com.thp.sqlsaas.entitlement.model.EntitlementContext;
import com.thp.sqlsaas.entitlement.model.EntitlementDecision;
import com.thp.sqlsaas.persistence.entity.QueryExecution.QueryState;
import com.thp.sqlsaas.persistence.service.QueryExecutionService;
import com.thp.sqlsaas.server.model.QueryExecutionResult;
import com.thp.sqlsaas.server.model.QueryPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Query Orchestrator - Central coordinator for query execution.
 * Responsibilities:
 * - Track query execution state in database
 * - Execute queries against connectors
 * - Apply entitlement checks
 * - Handle rate limiting
 * - Aggregate results (future: for joins)
 */
@Component
public class QueryOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryOrchestrator.class);
    
    private final ConnectorFactory connectorFactory;
    private final EntitlementService entitlementService;
    private final RateLimitService rateLimitService;
    private final QueryExecutionService queryExecutionService;
    
    public QueryOrchestrator(
            ConnectorFactory connectorFactory,
            EntitlementService entitlementService,
            RateLimitService rateLimitService,
            QueryExecutionService queryExecutionService) {
        this.connectorFactory = connectorFactory;
        this.entitlementService = entitlementService;
        this.rateLimitService = rateLimitService;
        this.queryExecutionService = queryExecutionService;
    }
    
    /**
     * Execute a query plan synchronously.
     * For now, we handle single-source queries only.
     * Future: support joins across multiple sources.
     */
    public QueryExecutionResult execute(QueryPlan plan) {
        logger.info("Executing query plan for tenant: {}, user: {}", 
                   plan.getTenantId(), plan.getUserId());
        
        long startTime = System.currentTimeMillis();
        String traceId = plan.getTraceId();
        
        try {
            // Step 0: Create execution record in database
            queryExecutionService.createExecution(
                traceId,
                plan.getTenantId(),
                plan.getUserId(),
                plan.getSqlQuery(), // Use SQL from plan
                plan.getConnectorType().name(),
                plan.getResource()
            );
            
            // Step 1: Check entitlements
            queryExecutionService.updateState(traceId, QueryState.VALIDATING);
            EntitlementDecision decision = checkEntitlements(plan);
            if (!decision.isAllowed()) {
                long executionTime = System.currentTimeMillis() - startTime;
                queryExecutionService.failExecution(
                    traceId,
                    "ENTITLEMENT_DENIED",
                    "Access denied: " + decision.getDenialReason(),
                    executionTime
                );
                return QueryExecutionResult.error(
                    "ENTITLEMENT_DENIED",
                    "Access denied: " + decision.getDenialReason(),
                    executionTime
                );
            }
            
            // Step 2: Check rate limits
            RateLimitDecision rateLimitDecision = rateLimitService.checkRateLimit(
                plan.getTenantId(), 
                plan.getUserId(), 
                plan.getConnectorType()
            );
            
            if (!rateLimitDecision.isAllowed()) {
                long executionTime = System.currentTimeMillis() - startTime;
                queryExecutionService.updateState(traceId, QueryState.RATE_LIMITED);
                queryExecutionService.failExecution(
                    traceId,
                    "RATE_LIMIT_EXCEEDED",
                    rateLimitDecision.getMessage(),
                    executionTime
                );
                return QueryExecutionResult.rateLimitExceeded(
                    rateLimitDecision.getRetryAfterSeconds(),
                    rateLimitDecision.getMessage()
                );
            }
            
            // Step 3: Execute against connector
            queryExecutionService.updateState(traceId, QueryState.EXECUTING);
            QueryExecutionResult result = executeOnConnector(plan, decision);
            
            // Step 4: Update execution record with results
            long executionTime = System.currentTimeMillis() - startTime;
            if ("SUCCESS".equals(result.getStatus())) {
                queryExecutionService.completeExecution(
                    traceId,
                    result.getStatus(),
                    result.getRows() != null ? result.getRows().size() : 0,
                    executionTime,
                    result.getFreshnessMs(),
                    false // cacheHit - should be passed from caller
                );
            } else {
                queryExecutionService.failExecution(
                    traceId,
                    result.getErrorCode(),
                    result.getErrorMessage(),
                    executionTime
                );
            }
            
            // Step 5: Record metrics
            recordMetrics(plan, result, executionTime);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing query", e);
            long executionTime = System.currentTimeMillis() - startTime;
            queryExecutionService.failExecution(
                traceId,
                "EXECUTION_ERROR",
                "Query execution failed: " + e.getMessage(),
                executionTime
            );
            return QueryExecutionResult.error(
                "EXECUTION_ERROR",
                "Query execution failed: " + e.getMessage(),
                executionTime
            );
        }
    }
    
    /**
     * Check entitlements for the query.
     */
    private EntitlementDecision checkEntitlements(QueryPlan plan) {
        EntitlementContext context = EntitlementContext.builder().roles(plan.getUserRoles()).tenantId(plan.getTenantId()).userId(plan.getUserId()).resource(
            plan.getResource()).requestedColumns(new HashSet<>(plan.getRequestedColumns())).build();
        return entitlementService.evaluateAccess(context);
    }
    
    /**
     * Execute the query on the appropriate connector.
     */
    private QueryExecutionResult executeOnConnector(
            QueryPlan plan, 
            EntitlementDecision decision) {
        
        Connector connector = null;
        try {
            // Get connector instance
            connector = connectorFactory.getConnector(plan.getConnectorType());
            
            // Connect (in real scenario, we'd cache connections)
            Connector.ConnectRequest connectRequest = new Connector.ConnectRequest(
                plan.getTenantId(),
                plan.getConnectorConfig()
            );
            Connector.ConnectResult connectResult = connector.connect(connectRequest);
            
            // Apply entitlement filters to the query
            List<Connector.Predicate> predicates = new ArrayList<>(plan.getPredicates());
            decision.getRowFilters().forEach(filter -> {
                predicates.add(new Connector.Predicate(
                    filter.getColumnName(),
                    filter.getOperator().getSql(),
                    filter.getValue()
                ));
            });
            
            // Filter columns based on entitlements
            List<String> allowedColumns = filterColumns(
                plan.getRequestedColumns(),
                decision.getAllowedColumns()
            );
            
            // Execute scan
            Connector.ExecuteScanRequest scanRequest = new Connector.ExecuteScanRequest(
                plan.getTenantId(),
                plan.getResource(),
                allowedColumns,
                predicates,
                plan.getLimit(),
                null, // pageToken - for pagination
                plan.getMaxStalenessMs()
            );
            
            Connector.RowPage rowPage = connector.executeScan(scanRequest);
            
            // Apply column masking
            List<Map<String, Object>> maskedRows = applyColumnMasking(
                rowPage.rows(),
                decision.getColumnMasks()
            );
            
            return QueryExecutionResult.success(
                maskedRows,
                rowPage.nextPageToken(),
                rowPage.freshnessMs(),
                "RATE_LIMIT_OK"
            );
            
        } catch (Exception e) {
            logger.error("Error executing on connector", e);
            return QueryExecutionResult.error(
                "CONNECTOR_ERROR",
                "Connector execution failed: " + e.getMessage(),
                0L
            );
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    logger.warn("Error closing connector", e);
                }
            }
        }
    }
    
    /**
     * Filter columns based on entitlements.
     */
    private List<String> filterColumns(
            List<String> requestedColumns, 
            Set<String> allowedColumns) {
        
        if (allowedColumns == null || allowedColumns.isEmpty()) {
            return requestedColumns;
        }
        
        return requestedColumns.stream()
            .filter(allowedColumns::contains)
            .toList();
    }
    
    /**
     * Apply column masking to the result rows.
     */
    private List<Map<String, Object>> applyColumnMasking(
            List<Map<String, Object>> rows,
            Map<String, ColumnMask> columnMasks) {
        
        if (columnMasks == null || columnMasks.isEmpty()) {
            return rows;
        }
        
        return rows.stream()
            .map(row -> {
                Map<String, Object> maskedRow = new HashMap<>(row);
                columnMasks.forEach((column, maskValue) -> {
                    if (maskedRow.containsKey(column)) {
                        maskedRow.put(column, maskValue);
                    }
                });
                return maskedRow;
            })
            .toList();
    }
    
    /**
     * Record metrics for observability.
     */
    private void recordMetrics(QueryPlan plan, QueryExecutionResult result, long executionTimeMs) {
        logger.info("Query executed - tenant: {}, user: {}, resource: {}, " +
                   "status: {}, rows: {}, executionTime: {}ms",
                   plan.getTenantId(),
                   plan.getUserId(),
                   plan.getResource(),
                   result.getStatus(),
                   result.getRows() != null ? result.getRows().size() : 0,
                   executionTimeMs);
    }
}
