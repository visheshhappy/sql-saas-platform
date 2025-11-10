package com.thp.sqlsaas.connector;

import com.thp.sqlsaas.connector.impl.GitHubMockConnector;
import com.thp.sqlsaas.connector.impl.JiraMockConnector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating connector instances.
 * Supports both mock and real connectors.
 */
@Component
public class ConnectorFactory {
    
    private final Map<String, Connector> activeConnectors = new HashMap<>();
    
    /**
     * Get or create a connector instance for the given type.
     * Simple version without tenant isolation for now.
     */
    public Connector getConnector(ConnectorType type) throws ConnectorException {
        String key = type.getId();
        return activeConnectors.computeIfAbsent(key, k -> createConnector(type));
    }
    
    /**
     * Get or create a connector instance for the given type and tenant.
     */
    public Connector getConnector(ConnectorType type, String tenantId) throws ConnectorException {
        String key = type.getId() + ":" + tenantId;
        
        return activeConnectors.computeIfAbsent(key, k -> createConnector(type));
    }
    
    /**
     * Create a new connector instance based on type.
     */
    private Connector createConnector(ConnectorType type) {
        return switch (type) {
            case GITHUB -> new GitHubMockConnector();
            case JIRA -> new JiraMockConnector();
            case SALESFORCE -> throw new UnsupportedOperationException("Salesforce connector not implemented");
            case ZENDESK -> throw new UnsupportedOperationException("Zendesk connector not implemented");
            case SLACK -> throw new UnsupportedOperationException("Slack connector not implemented");
            case NOTION -> throw new UnsupportedOperationException("Notion connector not implemented");
        };
    }
    
    /**
     * Close and remove a connector.
     */
    public void closeConnector(ConnectorType type, String tenantId) {
        String key = type.getId() + ":" + tenantId;
        Connector connector = activeConnectors.remove(key);
        if (connector != null) {
            connector.close();
        }
    }
    
    /**
     * Close all connectors.
     */
    public void closeAll() {
        activeConnectors.values().forEach(Connector::close);
        activeConnectors.clear();
    }
}
