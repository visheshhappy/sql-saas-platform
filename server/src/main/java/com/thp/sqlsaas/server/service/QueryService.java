package com.thp.sqlsaas.server.service;

import com.thp.sqlparser.SqlToModelConverter;
import com.thp.sqlsaas.connector.Connector;
import com.thp.sqlsaas.connector.ConnectorType;
import com.thp.sqlsaas.model.Filter;
import com.thp.sqlsaas.model.SqlQueryRequest;
import com.thp.sqlsaas.server.cache.CacheService;
import com.thp.sqlsaas.server.model.QueryExecutionResult;
import com.thp.sqlsaas.server.model.QueryPlan;
import com.thp.sqlsaas.server.orchestrator.QueryOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Query Service - Handles SQL query parsing and planning.
 * 
 * Flow:
 * 1. Check cache for cached results
 * 2. If cache miss, parse SQL and build query plan
 * 3. Execute via orchestrator
 * 4. Cache the results
 */
@Service
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    private final QueryOrchestrator queryOrchestrator;
    private final CacheService cacheService;
    private final com.thp.sqlsaas.persistence.service.UserService userService;
    
    // Table to connector type mapping
    private final Map<String, ConnectorType> tableToConnectorMapping = Map.of(
        "github_issues", ConnectorType.GITHUB,
        "github_pulls", ConnectorType.GITHUB,
        "jira_issues", ConnectorType.JIRA,
        "jira_projects", ConnectorType.JIRA
    );
    
    public QueryService(QueryOrchestrator queryOrchestrator, CacheService cacheService, 
                        com.thp.sqlsaas.persistence.service.UserService userService) {
        this.queryOrchestrator = queryOrchestrator;
        this.cacheService = cacheService;
        this.userService = userService;
    }
    
    /**
     * Execute SQL query.
     * Steps:
     * 2. Check cache
     * 3. If cache miss, parse SQL to extract table, columns, filters
     * 4. Build query plan
     * 5. Execute via orchestrator
     * 6. Cache the result
     */
    public QueryExecutionResult executeQuery(
            String sql,
            String tenantId,
            String userId,
            Long maxStalenessMs) {
        
        logger.info("Executing SQL query for tenant: {}, user: {}", tenantId, userId);
        logger.debug("SQL: {}", sql);
        
        try {
            Set<String> actualUserRoles;
            try {
                actualUserRoles = userService.getUserRoles(userId, tenantId);
                logger.info("User {} has roles: {} (from database)", userId, actualUserRoles);
            } catch (SecurityException e) {
                logger.error("Security error: User {} not found in tenant {}", userId, tenantId);
                return QueryExecutionResult.error(
                    "AUTHENTICATION_FAILED",
                    "User not found or not authorized for this tenant",
                    0L
                );
            }
            
            // Step 1: Check cache
            String cacheKey = CacheService.generateCacheKey(tenantId, userId, sql);
            QueryExecutionResult cachedResult = cacheService.get(cacheKey, maxStalenessMs);
            
            if (cachedResult != null) {
                logger.info("Cache hit for query - tenant: {}, user: {}", tenantId, userId);
                return cachedResult;
            }
            
            logger.info("Cache miss - executing query against connector");
            
            // Step 2: Parse SQL
            SqlQueryRequest sqlRequest = SqlToModelConverter.parseAndConvert(sql);
            
            // Step 3: Determine connector type from table name
            String tableName = sqlRequest.getTableName();
            ConnectorType connectorType = tableToConnectorMapping.get(tableName.toLowerCase());
            
            if (connectorType == null) {
                return QueryExecutionResult.error(
                    "INVALID_TABLE",
                    "Table not found: " + tableName + ". Available tables: " + 
                        tableToConnectorMapping.keySet(),
                    0L
                );
            }
            
            // Step 4: Map table to resource
            String resource = mapTableToResource(tableName);
            
            // Step 5: Convert filters to predicates
            List<Connector.Predicate> predicates = convertFiltersToPredicates(
                sqlRequest.getFilters()
            );
            
            // Step 6: Build query plan (using REAL roles from database)
            QueryPlan plan = buildQueryPlan(
                sql,
                tenantId,
                userId,
                actualUserRoles,  // ← Use roles from database!
                connectorType,
                resource,
                List.of("*"), // For now, select all columns
                predicates,
                100, // default limit
                maxStalenessMs
            );
            
            // Step 7: Execute via orchestrator
            QueryExecutionResult result = queryOrchestrator.execute(plan);
            
            // Step 8: Cache successful results
            if ("SUCCESS".equals(result.getStatus())) {
                cacheService.put(cacheKey, result);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing query", e);
            return QueryExecutionResult.error(
                "QUERY_PARSE_ERROR",
                "Failed to parse or execute query: " + e.getMessage(),
                0L
            );
        }
    }
    
    /**
     * Build a query plan from parsed query components.
     */
    private QueryPlan buildQueryPlan(
            String sqlQuery,
            String tenantId,
            String userId,
            Set<String> userRoles,
            ConnectorType connectorType,
            String resource,
            List<String> columns,
            List<Connector.Predicate> predicates,
            Integer limit,
            Long maxStalenessMs) {
        
        // In a real implementation, connector config would come from a registry
        Map<String, String> connectorConfig = Map.of(
            "type", "mock",
            "tenantId", tenantId
        );
        
        String traceId = UUID.randomUUID().toString();
        
        return new QueryPlan(
            tenantId,
            userId,
            userRoles,
            connectorType,
            connectorConfig,
            sqlQuery,
            resource,
            columns,
            predicates,
            limit,
            maxStalenessMs,
            traceId
        );
    }
    
    /**
     * Map table name to connector resource.
     * e.g., "github_issues" -> "issues"
     */
    private String mapTableToResource(String tableName) {
        String[] parts = tableName.split("_");
        if (parts.length > 1) {
            return parts[1]; // "github_issues" -> "issues"
        }
        return tableName;
    }
    
    /**
     * Convert domain Filter objects to Connector Predicate objects.
     */
    private List<Connector.Predicate> convertFiltersToPredicates(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return new ArrayList<>();
        }
        
        return filters.stream()
            .map(filter -> new Connector.Predicate(
                filter.getColumnName(),
                mapOperatorToString(filter.getOperator()),
                filter.getValue()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Map FilterOperator enum to string representation.
     */
    private String mapOperatorToString(com.thp.sqlsaas.model.FilterOperator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_OR_EQUAL -> "<=";
            case LIKE -> "LIKE";
            case NOT_LIKE -> "NOT LIKE";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
            case BETWEEN -> "BETWEEN";
            case IS_NULL -> "IS NULL";
            case IS_NOT_NULL -> "IS NOT NULL";
        };
    }
}
