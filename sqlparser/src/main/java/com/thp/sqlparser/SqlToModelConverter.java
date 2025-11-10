package com.thp.sqlparser;

import com.thp.sqlsaas.model.Filter;
import com.thp.sqlsaas.model.FilterOperator;
import com.thp.sqlsaas.model.SqlQueryRequest;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts JSqlParser parsed SQL into our domain model (SqlQueryRequest).
 */
public class SqlToModelConverter {
    
    /**
     * Parse SQL string and convert to SqlQueryRequest model.
     */
    public static SqlQueryRequest parseAndConvert(String sql) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        if (!(statement instanceof Select)) {
            throw new IllegalArgumentException("Only SELECT statements are supported");
        }
        
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        SqlQueryRequest request = new SqlQueryRequest();
        
        // Extract table name
        if (plainSelect.getFromItem() instanceof Table) {
            Table table = (Table) plainSelect.getFromItem();
            request.setTableName(table.getName());
        }
        
        // Extract filters from WHERE clause
        Expression where = plainSelect.getWhere();
        if (where != null) {
            List<Filter> filters = extractFilters(where);
            request.setFilters(filters);
        }
        
        return request;
    }
    
    /**
     * Recursively extract filters from WHERE expression.
     */
    private static List<Filter> extractFilters(Expression expression) {
        List<Filter> filters = new ArrayList<>();
        
        if (expression instanceof AndExpression) {
            // Handle AND conditions
            AndExpression andExpr = (AndExpression) expression;
            filters.addAll(extractFilters(andExpr.getLeftExpression()));
            filters.addAll(extractFilters(andExpr.getRightExpression()));
        } else if (expression instanceof ComparisonOperator) {
            // Handle comparison operators (=, >, <, etc.)
            Filter filter = extractComparisonFilter((ComparisonOperator) expression);
            if (filter != null) {
                filters.add(filter);
            }
        } else if (expression instanceof LikeExpression) {
            // Handle LIKE
            Filter filter = extractLikeFilter((LikeExpression) expression);
            if (filter != null) {
                filters.add(filter);
            }
        } else if (expression instanceof InExpression) {
            // Handle IN
            Filter filter = extractInFilter((InExpression) expression);
            if (filter != null) {
                filters.add(filter);
            }
        } else if (expression instanceof Between) {
            // Handle BETWEEN
            Filter filter = extractBetweenFilter((Between) expression);
            if (filter != null) {
                filters.add(filter);
            }
        } else if (expression instanceof IsNullExpression) {
            // Handle IS NULL / IS NOT NULL
            Filter filter = extractIsNullFilter((IsNullExpression) expression);
            if (filter != null) {
                filters.add(filter);
            }
        }
        
        return filters;
    }
    
    private static Filter extractComparisonFilter(ComparisonOperator expr) {
        Expression left = expr.getLeftExpression();
        Expression right = expr.getRightExpression();
        
        if (!(left instanceof Column)) {
            return null;
        }
        
        String columnName = ((Column) left).getColumnName();
        Object value = extractValue(right);
        FilterOperator operator = mapOperator(expr);
        
        return new Filter(columnName, operator, value);
    }
    
    private static Filter extractLikeFilter(LikeExpression expr) {
        Expression left = expr.getLeftExpression();
        Expression right = expr.getRightExpression();
        
        if (!(left instanceof Column)) {
            return null;
        }
        
        String columnName = ((Column) left).getColumnName();
        Object value = extractValue(right);
        FilterOperator operator = expr.isNot() ? FilterOperator.NOT_LIKE : FilterOperator.LIKE;
        
        return new Filter(columnName, operator, value);
    }
    
    private static Filter extractInFilter(InExpression expr) {
        Expression left = expr.getLeftExpression();
        
        if (!(left instanceof Column)) {
            return null;
        }
        
        String columnName = ((Column) left).getColumnName();
        FilterOperator operator = expr.isNot() ? FilterOperator.NOT_IN : FilterOperator.IN;
        
        // For IN clause, we'll store the entire right expression as the value
        Object value = expr.getRightExpression().toString();
        
        return new Filter(columnName, operator, value);
    }
    
    private static Filter extractBetweenFilter(Between expr) {
        Expression left = expr.getLeftExpression();
        
        if (!(left instanceof Column)) {
            return null;
        }
        
        String columnName = ((Column) left).getColumnName();
        String value = expr.getBetweenExpressionStart() + " AND " + expr.getBetweenExpressionEnd();
        
        return new Filter(columnName, FilterOperator.BETWEEN, value);
    }
    
    private static Filter extractIsNullFilter(IsNullExpression expr) {
        Expression left = expr.getLeftExpression();
        
        if (!(left instanceof Column)) {
            return null;
        }
        
        String columnName = ((Column) left).getColumnName();
        FilterOperator operator = expr.isNot() ? FilterOperator.IS_NOT_NULL : FilterOperator.IS_NULL;
        
        return new Filter(columnName, operator, null);
    }
    
    private static FilterOperator mapOperator(ComparisonOperator expr) {
        if (expr instanceof EqualsTo) {
            return FilterOperator.EQUALS;
        } else if (expr instanceof NotEqualsTo) {
            return FilterOperator.NOT_EQUALS;
        } else if (expr instanceof GreaterThan) {
            return FilterOperator.GREATER_THAN;
        } else if (expr instanceof GreaterThanEquals) {
            return FilterOperator.GREATER_THAN_OR_EQUAL;
        } else if (expr instanceof MinorThan) {
            return FilterOperator.LESS_THAN;
        } else if (expr instanceof MinorThanEquals) {
            return FilterOperator.LESS_THAN_OR_EQUAL;
        }
        return FilterOperator.EQUALS; // default
    }
    
    private static Object extractValue(Expression expr) {
        if (expr instanceof StringValue) {
            return ((StringValue) expr).getValue();
        } else if (expr instanceof LongValue) {
            return ((LongValue) expr).getValue();
        } else if (expr instanceof DoubleValue) {
            return ((DoubleValue) expr).getValue();
        } else if (expr instanceof DateValue) {
            return ((DateValue) expr).getValue();
        } else if (expr instanceof TimeValue) {
            return ((TimeValue) expr).getValue();
        } else if (expr instanceof TimestampValue) {
            return ((TimestampValue) expr).getValue();
        } else if (expr instanceof NullValue) {
            return null;
        }
        // For other types, return string representation
        return expr.toString();
    }
}
