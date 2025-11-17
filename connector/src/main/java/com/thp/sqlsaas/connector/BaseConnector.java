package com.thp.sqlsaas.connector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base implementation providing common functionality for all connectors.
 */
public abstract class BaseConnector implements Connector {
    
    protected Map<String, Object> sessionContext;
    protected boolean connected = false;
    
    /**
     * Get the connector type identifier.
     */
    public abstract String getConnectorId();
    
    /**
     * Get the display name of this connector.
     */
    public abstract String getDisplayName();
    
    @Override
    public ConnectResult connect(ConnectRequest req) throws ConnectorException {
        try {
            // Validate configuration
            validateConfig(req.config());
            
            // Perform connection logic (overridden by subclasses)
            sessionContext = performConnect(req);
            connected = true;
            
            // Build capability descriptor
            CapabilityDescriptor capabilities = buildCapabilities();
            
            // Get allowed repositories/resources
            List<String> resources = getAllowedResources(req);
            
            return new ConnectResult(capabilities, resources, sessionContext);
            
        } catch (Exception e) {
            throw new ConnectorException(
                ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                "Failed to connect to " + getDisplayName() + ": " + e.getMessage(),
                getConnectorId(),
                e
            );
        }
    }
    
    @Override
    public RowPage executeScan(ExecuteScanRequest req) throws ConnectorException {
        if (!connected) {
            throw new ConnectorException(
                ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                "Connector not connected. Call connect() first.",
                getConnectorId()
            );
        }
        
        try {
            // Apply predicate filtering
            List<Map<String, Object>> allRows = fetchAllRows(req.resource());
            List<Map<String, Object>> filteredRows = applyPredicates(allRows, req.predicates());
            
            // Apply column projection
            filteredRows = applyProjection(filteredRows, req.columns());
            
            // Apply pagination
            PaginationResult paginationResult = applyPagination(
                filteredRows, 
                req.limit(), 
                req.pageToken()
            );
            
            // Calculate freshness (mock: always 0ms for fresh data)
            long freshnessMs = calculateFreshness(req.maxStalenessMs());
            
            return new RowPage(
                paginationResult.rows(),
                paginationResult.nextPageToken(),
                freshnessMs
            );
            
        } catch (Exception e) {
            throw new ConnectorException(
                ConnectorException.ErrorCode.UNKNOWN_ERROR,
                "Failed to execute scan on " + getDisplayName() + ": " + e.getMessage(),
                getConnectorId(),
                e
            );
        }
    }
    
    @Override
    public void close() {
        connected = false;
        sessionContext = null;
    }
    
    /**
     * Validate connector configuration.
     */
    protected void validateConfig(Map<String, String> config) throws ConnectorException {
        // Override in subclasses if needed
    }
    
    /**
     * Perform actual connection logic. Override in subclasses.
     */
    protected abstract Map<String, Object> performConnect(ConnectRequest req) throws Exception;
    
    /**
     * Build capability descriptor for this connector.
     */
    protected abstract CapabilityDescriptor buildCapabilities();
    
    /**
     * Get list of allowed resources for this connection.
     */
    protected abstract List<String> getAllowedResources(ConnectRequest req);
    
    /**
     * Fetch all rows for a given resource. Override in subclasses.
     */
    protected abstract List<Map<String, Object>> fetchAllRows(String resource) throws Exception;
    
    /**
     * Apply predicates to filter rows.
     */
    protected List<Map<String, Object>> applyPredicates(
            List<Map<String, Object>> rows, 
            List<Predicate> predicates) {
        
        if (predicates == null || predicates.isEmpty()) {
            return rows;
        }
        
        return rows.stream()
                .filter(row -> matchesAllPredicates(row, predicates))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a row matches all predicates.
     */
    protected boolean matchesAllPredicates(Map<String, Object> row, List<Predicate> predicates) {
        for (Predicate predicate : predicates) {
            if (!matchesPredicate(row, predicate)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if a row matches a single predicate.
     */
    protected boolean matchesPredicate(Map<String, Object> row, Predicate predicate) {
        Object value = row.get(predicate.field());
        Object predicateValue = predicate.value();
        
        if (value == null) {
            return false;
        }
        
        return switch (predicate.op().toUpperCase()) {
            case "=" -> value.equals(predicateValue);
            case "!=" -> !value.equals(predicateValue);
            case ">" -> compareValues(value, predicateValue) > 0;
            case "<" -> compareValues(value, predicateValue) < 0;
            case ">=" -> compareValues(value, predicateValue) >= 0;
            case "<=" -> compareValues(value, predicateValue) <= 0;
            case "IN" -> predicateValue instanceof Collection && 
                        ((Collection<?>) predicateValue).contains(value);
            case "LIKE" -> value.toString().contains(predicateValue.toString());
            default -> false;
        };
    }
    
    /**
     * Compare two values (handles numbers and strings).
     */
    @SuppressWarnings("unchecked")
    protected int compareValues(Object v1, Object v2) {
        if (v1 instanceof Comparable && v2 instanceof Comparable) {
            return ((Comparable) v1).compareTo(v2);
        }
        return v1.toString().compareTo(v2.toString());
    }
    
    /**
     * Apply column projection to rows.
     */
    protected List<Map<String, Object>> applyProjection(
            List<Map<String, Object>> rows,
            List<String> columns) {
        
        // If columns is null, empty, or contains wildcard, return all columns
        if (columns == null || columns.isEmpty() || columns.contains("*")) {
            return rows;
        }
        
        return rows.stream()
                .map(row -> projectColumns(row, columns))
                .collect(Collectors.toList());
    }
    
    /**
     * Project specific columns from a row.
     */
    protected Map<String, Object> projectColumns(Map<String, Object> row, List<String> columns) {
        Map<String, Object> projected = new HashMap<>();
        for (String column : columns) {
            if (row.containsKey(column)) {
                projected.put(column, row.get(column));
            }
        }
        return projected;
    }
    
    /**
     * Apply pagination to results.
     */
    protected PaginationResult applyPagination(
            List<Map<String, Object>> rows,
            Integer limit,
            String pageToken) {
        
        int startIndex = 0;
        if (pageToken != null && !pageToken.isEmpty()) {
            try {
                startIndex = Integer.parseInt(pageToken);
            } catch (NumberFormatException e) {
                // Invalid token, start from beginning
            }
        }
        
        int pageSize = (limit != null && limit > 0) ? limit : 100;
        int endIndex = Math.min(startIndex + pageSize, rows.size());
        
        List<Map<String, Object>> pageRows = rows.subList(startIndex, endIndex);
        
        String nextPageToken = null;
        if (endIndex < rows.size()) {
            nextPageToken = String.valueOf(endIndex);
        }
        
        return new PaginationResult(pageRows, nextPageToken);
    }
    
    /**
     * Calculate freshness in milliseconds.
     * For mock connectors, data is always fresh (0ms).
     */
    protected long calculateFreshness(Long maxStalenessMs) {
        // Mock implementation: always return 0 (perfectly fresh)
        return 0L;
    }
    
    /**
     * Helper record for pagination results.
     */
    protected record PaginationResult(List<Map<String, Object>> rows, String nextPageToken) {}
}
