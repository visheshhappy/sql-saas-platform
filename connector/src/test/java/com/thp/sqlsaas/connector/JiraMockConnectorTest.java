package com.thp.sqlsaas.connector;

import com.thp.sqlsaas.connector.impl.JiraMockConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JiraMockConnectorTest {
    
    private JiraMockConnector connector;
    
    @BeforeEach
    void setUp() {
        connector = new JiraMockConnector();
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
        assertTrue(result.repos().contains("PROJ1"));
        assertTrue(result.sessionContext().containsKey("instance_url"));
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
        assertTrue(capabilities.resources().contains("projects"));
        assertTrue(capabilities.resources().contains("users"));
        assertTrue(capabilities.columns().containsKey("issues"));
        assertTrue(capabilities.columns().get("issues").contains("summary"));
        assertTrue(capabilities.columns().get("issues").contains("status"));
    }
    
    @Test
    void testExecuteScan_AllIssues() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
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
        assertTrue(result.rows().size() >= 8); // We have at least 8 issues
    }
    
    @Test
    void testExecuteScan_FilterByStatus() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("status", "=", "In Progress")
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
            assertEquals("In Progress", row.get("status"));
        });
    }
    
    @Test
    void testExecuteScan_FilterByProject() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("project", "=", "PROJ1")
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
            assertEquals("PROJ1", row.get("project"));
        });
    }
    
    @Test
    void testExecuteScan_FilterByPriority() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("priority", "=", "High")
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
            assertEquals("High", row.get("priority"));
        });
    }
    
    @Test
    void testExecuteScan_ColumnProjection() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<String> columns = List.of("key", "summary", "status", "assignee");
        
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
            assertEquals(4, row.size());
            assertTrue(row.containsKey("key"));
            assertTrue(row.containsKey("summary"));
            assertTrue(row.containsKey("status"));
            assertTrue(row.containsKey("assignee"));
            assertFalse(row.containsKey("description")); // Not requested
        });
    }
    
    @Test
    void testExecuteScan_MultipleFilters() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("project", "=", "PROJ1"),
                new Connector.Predicate("status", "=", "In Progress")
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
        result.rows().forEach(row -> {
            assertEquals("PROJ1", row.get("project"));
            assertEquals("In Progress", row.get("status"));
        });
    }
    
    @Test
    void testExecuteScan_Projects() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "projects",
                List.of("key", "name", "lead"),
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
        assertEquals(3, result.rows().size()); // We have 3 projects
        result.rows().forEach(row -> {
            assertNotNull(row.get("key"));
            assertNotNull(row.get("name"));
            assertNotNull(row.get("lead"));
        });
    }
    
    @Test
    void testExecuteScan_Users() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "users",
                null,
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
            assertNotNull(row.get("email"));
            assertNotNull(row.get("display_name"));
        });
    }
    
    @Test
    void testExecuteScan_FilterActiveUsers() throws ConnectorException {
        // Given
        connector.connect(new Connector.ConnectRequest("test-tenant", Map.of()));
        
        List<Connector.Predicate> predicates = List.of(
                new Connector.Predicate("active", "=", true)
        );
        
        Connector.ExecuteScanRequest request = new Connector.ExecuteScanRequest(
                "test-tenant",
                "users",
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
            assertEquals(true, row.get("active"));
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
                5, // Limit 5
                null,
                null
        );
        
        // When - First page
        Connector.RowPage page1 = connector.executeScan(request);
        
        // Then
        assertNotNull(page1);
        assertEquals(5, page1.rows().size());
        assertNotNull(page1.nextPageToken());
        
        // When - Second page
        Connector.ExecuteScanRequest request2 = new Connector.ExecuteScanRequest(
                "test-tenant",
                "issues",
                null,
                null,
                5,
                page1.nextPageToken(),
                null
        );
        Connector.RowPage page2 = connector.executeScan(request2);
        
        // Then
        assertNotNull(page2);
        assertTrue(page2.rows().size() > 0);
    }
}
