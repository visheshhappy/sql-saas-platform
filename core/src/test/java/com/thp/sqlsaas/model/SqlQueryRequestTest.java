package com.thp.sqlsaas.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SqlQueryRequestTest {

    @Test
    void testCreateBasicRequest() {
        SqlQueryRequest request = new SqlQueryRequest();
        request.setTableName("users");
        
        Filter ageFilter = new Filter("age", FilterOperator.GREATER_THAN, 18);
        request.addFilter(ageFilter);
        
        assertEquals("users", request.getTableName());
        assertEquals(1, request.getFilters().size());
        assertEquals("age", request.getFilters().get(0).getColumnName());
        assertEquals(FilterOperator.GREATER_THAN, request.getFilters().get(0).getOperator());
        assertEquals(18, request.getFilters().get(0).getValue());
    }
    
    @Test
    void testCreateRequestWithMultipleFilters() {
        Filter statusFilter = new Filter("status", FilterOperator.EQUALS, "active");
        Filter salaryFilter = new Filter("salary", FilterOperator.GREATER_THAN_OR_EQUAL, 50000);
        
        SqlQueryRequest request = new SqlQueryRequest("employees", 
                Arrays.asList(statusFilter, salaryFilter));
        
        assertEquals("employees", request.getTableName());
        assertEquals(2, request.getFilters().size());
    }
    
    @Test
    void testFilterEquality() {
        Filter filter1 = new Filter("age", FilterOperator.EQUALS, 25);
        Filter filter2 = new Filter("age", FilterOperator.EQUALS, 25);
        Filter filter3 = new Filter("age", FilterOperator.GREATER_THAN, 25);
        
        assertEquals(filter1, filter2);
        assertNotEquals(filter1, filter3);
    }
    
    @Test
    void testRequestToString() {
        Filter filter = new Filter("name", FilterOperator.LIKE, "John%");
        SqlQueryRequest request = new SqlQueryRequest("customers", Arrays.asList(filter));
        
        String result = request.toString();
        
        assertTrue(result.contains("customers"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("LIKE"));
    }
    
    @Test
    void testFilterOperatorFromString() {
        assertEquals(FilterOperator.EQUALS, FilterOperator.fromString("="));
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.fromString("!="));
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.fromString("<>"));
        assertEquals(FilterOperator.GREATER_THAN, FilterOperator.fromString(">"));
        assertEquals(FilterOperator.LIKE, FilterOperator.fromString("LIKE"));
        assertEquals(FilterOperator.IN, FilterOperator.fromString("IN"));
    }
    
    @Test
    void testUnknownOperatorThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            FilterOperator.fromString("UNKNOWN_OP");
        });
    }
}
