package com.thp.sqlsaas.server.controller;

import com.thp.sqlsaas.server.model.QueryExecutionResult;
import com.thp.sqlsaas.server.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * REST Controller for query execution.
 * Endpoint: POST /v1/query
 */
@RestController
@RequestMapping("/v1")
public class QueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    private final QueryService queryService;
    
    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }
    
    /**
     * Execute a SQL query.
     * 
     * Request body:
     * {
     *   "sql": "SELECT * FROM github_issues WHERE state = 'open'",
     *   "tenantId": "tenant-123",
     *   "userId": "user-456",
     *   "userRoles": ["developer", "admin"],
     *   "maxStalenessMs": 60000
     * }
     */
    @PostMapping("/query")
    public ResponseEntity<QueryExecutionResult> executeQuery(
            @RequestBody QueryRequestDto request) {
        
        logger.info("Received query request - tenant: {}, user: {}", 
                   request.tenantId(), request.userId());
        
        try {
            QueryExecutionResult result = queryService.executeQuery(
                request.sql(),
                request.tenantId(),
                request.userId(),
                request.maxStalenessMs() != null ? request.maxStalenessMs() : 60000L
            );
            
            HttpStatus status = switch (result.getStatus()) {
                case "SUCCESS" -> HttpStatus.OK;
                case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
                case "ERROR" -> {
                    if ("ENTITLEMENT_DENIED".equals(result.getErrorCode())) {
                        yield HttpStatus.FORBIDDEN;
                    }
                    yield HttpStatus.BAD_REQUEST;
                }
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            
            return ResponseEntity.status(status).body(result);
            
        } catch (Exception e) {
            logger.error("Error processing query request", e);
            QueryExecutionResult errorResult = QueryExecutionResult.error(
                "INTERNAL_ERROR",
                "Internal server error: " + e.getMessage(),
                0L
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "query-service"
        ));
    }
    
    /**
     * DTO for query request.
     */
    public record QueryRequestDto(
        String sql,
        String tenantId,
        String userId,
        Long maxStalenessMs
    ) {}
}
