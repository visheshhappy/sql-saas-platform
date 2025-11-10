package com.thp.sqlparser;

import com.thp.sqlsaas.model.Filter;
import com.thp.sqlsaas.model.FilterOperator;
import com.thp.sqlsaas.model.SqlQueryRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlToModelConverterTest {

    @Test
    void testSimpleSelectWithSingleFilter() throws Exception {
        String sql = "SELECT * FROM users WHERE age > 18";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("users", request.getTableName());
        assertEquals(1, request.getFilters().size());
        
        Filter filter = request.getFilters().get(0);
        assertEquals("age", filter.getColumnName());
        assertEquals(FilterOperator.GREATER_THAN, filter.getOperator());
        assertEquals(18L, filter.getValue());
    }
    
    @Test
    void testSelectWithMultipleFilters() throws Exception {
        String sql = "SELECT id, name FROM employees WHERE status = 'active' AND salary >= 50000";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("employees", request.getTableName());
        assertEquals(2, request.getFilters().size());
        
        // First filter
        Filter filter1 = request.getFilters().get(0);
        assertEquals("status", filter1.getColumnName());
        assertEquals(FilterOperator.EQUALS, filter1.getOperator());
        assertEquals("active", filter1.getValue());
        
        // Second filter
        Filter filter2 = request.getFilters().get(1);
        assertEquals("salary", filter2.getColumnName());
        assertEquals(FilterOperator.GREATER_THAN_OR_EQUAL, filter2.getOperator());
        assertEquals(50000L, filter2.getValue());
    }
    
    @Test
    void testSelectWithLikeOperator() throws Exception {
        String sql = "SELECT * FROM customers WHERE name LIKE 'John%'";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("customers", request.getTableName());
        assertEquals(1, request.getFilters().size());
        
        Filter filter = request.getFilters().get(0);
        assertEquals("name", filter.getColumnName());
        assertEquals(FilterOperator.LIKE, filter.getOperator());
        assertEquals("John%", filter.getValue());
    }
    
    @Test
    void testSelectWithAllComparisonOperators() throws Exception {
        // Test equals
        SqlQueryRequest req1 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id = 10");
        assertEquals(FilterOperator.EQUALS, req1.getFilters().get(0).getOperator());
        
        // Test not equals
        SqlQueryRequest req2 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id != 10");
        assertEquals(FilterOperator.NOT_EQUALS, req2.getFilters().get(0).getOperator());
        
        // Test greater than
        SqlQueryRequest req3 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id > 10");
        assertEquals(FilterOperator.GREATER_THAN, req3.getFilters().get(0).getOperator());
        
        // Test less than
        SqlQueryRequest req4 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id < 10");
        assertEquals(FilterOperator.LESS_THAN, req4.getFilters().get(0).getOperator());
        
        // Test greater than or equal
        SqlQueryRequest req5 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id >= 10");
        assertEquals(FilterOperator.GREATER_THAN_OR_EQUAL, req5.getFilters().get(0).getOperator());
        
        // Test less than or equal
        SqlQueryRequest req6 = SqlToModelConverter.parseAndConvert("SELECT * FROM t WHERE id <= 10");
        assertEquals(FilterOperator.LESS_THAN_OR_EQUAL, req6.getFilters().get(0).getOperator());
    }
    
    @Test
    void testSelectWithThreeFilters() throws Exception {
        String sql = "SELECT * FROM products WHERE price > 100 AND category = 'electronics' AND stock < 50";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("products", request.getTableName());
        assertEquals(3, request.getFilters().size());
        
        assertEquals("price", request.getFilters().get(0).getColumnName());
        assertEquals(FilterOperator.GREATER_THAN, request.getFilters().get(0).getOperator());
        
        assertEquals("category", request.getFilters().get(1).getColumnName());
        assertEquals(FilterOperator.EQUALS, request.getFilters().get(1).getOperator());
        
        assertEquals("stock", request.getFilters().get(2).getColumnName());
        assertEquals(FilterOperator.LESS_THAN, request.getFilters().get(2).getOperator());
    }
    
    @Test
    void testSelectWithTableAlias() throws Exception {
        String sql = "SELECT u.id, u.name FROM users u WHERE u.age > 21";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("users", request.getTableName());
        assertEquals(1, request.getFilters().size());
    }
    
    @Test
    void testSelectWithNoFilters() throws Exception {
        String sql = "SELECT * FROM orders";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("orders", request.getTableName());
        assertTrue(request.getFilters().isEmpty());
    }
    
    @Test
    void testSelectWithIsNull() throws Exception {
        String sql = "SELECT * FROM users WHERE email IS NULL";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        assertEquals("users", request.getTableName());
        assertEquals(1, request.getFilters().size());
        
        Filter filter = request.getFilters().get(0);
        assertEquals("email", filter.getColumnName());
        assertEquals(FilterOperator.IS_NULL, filter.getOperator());
        assertNull(filter.getValue());
    }
    
    @Test
    void testSelectWithIsNotNull() throws Exception {
        String sql = "SELECT * FROM users WHERE email IS NOT NULL";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        Filter filter = request.getFilters().get(0);
        assertEquals(FilterOperator.IS_NOT_NULL, filter.getOperator());
    }
    
    @Test
    void testPrintRequestDetails() throws Exception {
        String sql = "SELECT * FROM customers WHERE age > 25 AND city = 'New York'";
        
        SqlQueryRequest request = SqlToModelConverter.parseAndConvert(sql);
        
        System.out.println("Parsed SQL Request:");
        System.out.println("Table: " + request.getTableName());
        System.out.println("Filters:");
        for (Filter filter : request.getFilters()) {
            System.out.println("  - " + filter.getColumnName() + " " + 
                             filter.getOperator().getSymbol() + " " + filter.getValue());
        }
        
        // This test just demonstrates the output
        assertNotNull(request);
    }
}
