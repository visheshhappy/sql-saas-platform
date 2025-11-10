package com.thp.sqlsaas.connector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for managing connector operations.
 * Provides high-level API for connector lifecycle and data access.
 */
@Service
public class ConnectorService {
    
    @Autowired
    private ConnectorFactory connectorFactory;
    
    /**
     * Connect to a data source and return connection metadata.
     */
    public Connector.ConnectResult connect(
            ConnectorType type, 
            String tenantId,
            Map<String, String> config) throws ConnectorException {
        
        Connector connector = connectorFactory.getConnector(type, tenantId);
        Connector.ConnectRequest request = new Connector.ConnectRequest(tenantId, config);
        return connector.connect(request);
    }
    
    /**
     * Execute a scan operation on a connector.
     */
    public Connector.RowPage executeScan(
            ConnectorType type,
            String tenantId,
            String resource,
            List<String> columns,
            List<Connector.Predicate> predicates,
            Integer limit,
            String pageToken,
            Long maxStalenessMs) throws ConnectorException {
        
        Connector connector = connectorFactory.getConnector(type, tenantId);
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                tenantId,
                resource,
                columns,
                predicates,
                limit,
                pageToken,
                maxStalenessMs
        );
        
        return connector.executeScan(request);
    }
    
    /**
     * Get capabilities for a connector type.
     */
    public Connector.CapabilityDescriptor getCapabilities(
            ConnectorType type,
            String tenantId) throws ConnectorException {
        
        Connector connector = connectorFactory.getConnector(type, tenantId);
        
        // Ensure connector is connected first
        Map<String, String> config = Map.of();
        Connector.ConnectResult result = connector.connect(
                new Connector.ConnectRequest(tenantId, config)
        );
        
        return result.capabilities();
    }
    
    /**
     * Close a connector and release resources.
     */
    public void closeConnector(ConnectorType type, String tenantId) {
        connectorFactory.closeConnector(type, tenantId);
    }
    
    /**
     * Close all active connectors.
     */
    public void closeAll() {
        connectorFactory.closeAll();
    }
    
    /**
     * Simple health check message.
     */
    public String getMessage() {
        return "Connector Service is ready with support for: GitHub, Jira";
    }
}
