package com.thp.sqlsaas.connector;

import com.thp.sqlsaas.connector.impl.GitHubMockConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GitHubMockConnectorTest {
    
    private GitHubMockConnector connector;
    
    @BeforeEach
    void setUp() {
        connector = new GitHubMockConnector();
    }
    
    @Test
    void testConnect() throws ConnectorException {
        // Given
        Connector.ConnectRequest request = new Connector.ConnectRequest(
                "test-tenant",
                Map.of()
        );
        
        // When
        Connector.ConnectResult result = connector.connect(request);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.capabilities());
        assertNotNull(result.repos());
        assertFalse(result.repos().isEmpty());
        assertTrue(result.repos().contains("org/repo1"));
    }
    
    @Test
    void testGetCapabilities() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        // When
        Connector.CapabilityDescriptor capabilities = 
                connector.connect(new Connector.ConnectRequest("test-tenant", Map.of())).capabilities();
        
        // Then
        assertTrue(capabilities.resources().contains("issues"));
        assertTrue(capabilities.resources().contains("pulls"));
        assertTrue(capabilities.columns().containsKey("issues"));
        assertTrue(capabilities.columns().get("issues").contains("title"));
        assertTrue(capabilities.pushdownableFields().containsKey("issues"));
    }
    
    @Test
    void testExecuteScan_AllIssues() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null, // All columns
                null, // No predicates
                null, // No limit
                null, // No page token
                null  // No staleness requirement
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        assertTrue(result.rows().size() >= 5); // We have at least 5 issues
        assertEquals(0L, result.freshnessMs()); // Mock data is always fresh
    }
    
    @Test
    void testExecuteScan_FilterByState() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("state", "=", "open")
        );
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                predicates,
                null,
                null,
                null
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        result.rows().forEach(row -> {
            assertEquals("open", row.get("state"));
        });
    }
    
    @Test
    void testExecuteScan_FilterByRepository() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("repository", "=", "org/repo1")
        );
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                predicates,
                null,
                null,
                null
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        result.rows().forEach(row -> {
            assertEquals("org/repo1", row.get("repository"));
        });
    }
    
    @Test
    void testExecuteScan_ColumnProjection() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<String> columns = List.of("id", "title", "state");
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                columns,
                null,
                null,
                null,
                null
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        result.rows().forEach(row -> {
            assertEquals(3, row.size());
            assertTrue(row.containsKey("id"));
            assertTrue(row.containsKey("title"));
            assertTrue(row.containsKey("state"));
            assertFalse(row.containsKey("body")); // Not requested
        });
    }
    
    @Test
    void testExecuteScan_Pagination() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                null,
                3, // Limit 3
                null,
                null
        );
        
        // When - First page
        Connector.RowPage page1 = connector.executeScan(request);
        
        // Then
        assertNotNull(page1);
        assertEquals(3, page1.rows().size());
        assertNotNull(page1.nextPageToken());
        
        // When - Second page
        Connector.ExecuteScanRequest request2 = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                null,
                3,
                page1.nextPageToken(),
                null
        );
        Connector.RowPage page2 = connector.executeScan(request2);
        
        // Then
        assertNotNull(page2);
        assertTrue(page2.rows().size() > 0);
    }
    
    @Test
    void testExecuteScan_PullRequests() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "pulls",
                List.of("id", "title", "state", "draft"),
                null,
                null,
                null,
                null
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        result.rows().forEach(row -> {
            assertNotNull(row.get("id"));
            assertNotNull(row.get("title"));
            assertNotNull(row.get("state"));
        });
    }
    
    @Test
    void testExecuteScan_FilterDraftPRs() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("draft", "=", false)
        );
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "pulls",
                null,
                predicates,
                null,
                null,
                null
        );
        
        // When
        Connector.RowPage result = connector.executeScan(request);
        
        // Then
        assertNotNull(result);
        assertFalse(result.rows().isEmpty());
        result.rows().forEach(row -> {
            assertEquals(false, row.get("draft"));
        });
    }
    
    @Test
    void testExecuteScan_WithoutConnection_ThrowsException() {
        // Given - no connection
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                null,
                null,
                null,
                null
        );
        
        // When/Then
        assertThrows(ConnectorException.class, () -> {
            connector.executeScan(request);
        });
    }
}
