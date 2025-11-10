import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JSqlParserTest {

    @Test
    void testSimpleSelect() throws Exception {
        String sql = "SELECT id, name, email FROM users WHERE age > 18";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(Select.class, statement);
        
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        assertNotNull(plainSelect);
        assertNotNull(plainSelect.getFromItem());
        
        Table table = (Table) plainSelect.getFromItem();
        assertEquals("users", table.getName());
        
        // Check columns
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        assertEquals(3, selectItems.size());
        
        // Check WHERE clause
        assertNotNull(plainSelect.getWhere());
        assertTrue(plainSelect.getWhere().toString().contains("age > 18"));
    }

    @Test
    void testSelectWithJoin() throws Exception {
        String sql = "SELECT u.id, u.name, o.order_id " +
                     "FROM users u " +
                     "INNER JOIN orders o ON u.id = o.user_id " +
                     "WHERE u.status = 'active'";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        // Check FROM table
        Table fromTable = (Table) plainSelect.getFromItem();
        assertEquals("users", fromTable.getName());
        assertEquals("u", fromTable.getAlias().getName());
        
        // Check JOINs
        List<Join> joins = plainSelect.getJoins();
        assertNotNull(joins);
        assertEquals(1, joins.size());
        
        Join join = joins.get(0);
        assertTrue(join.isInner());
        
        Table joinTable = (Table) join.getFromItem();
        assertEquals("orders", joinTable.getName());
        assertEquals("o", joinTable.getAlias().getName());
        
        // Check ON condition
        Expression onExpression = join.getOnExpression();
        assertNotNull(onExpression);
        assertTrue(onExpression.toString().contains("u.id = o.user_id"));
    }

    @Test
    void testSelectWithMultipleJoins() throws Exception {
        String sql = "SELECT e.name, d.dept_name, p.project_name " +
                     "FROM employees e " +
                     "LEFT JOIN departments d ON e.dept_id = d.id " +
                     "LEFT JOIN projects p ON e.project_id = p.id " +
                     "WHERE e.salary > 50000";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        // Check multiple joins
        List<Join> joins = plainSelect.getJoins();
        assertNotNull(joins);
        assertEquals(2, joins.size());
        
        // First join
        Join firstJoin = joins.get(0);
        assertTrue(firstJoin.isLeft());
        Table firstJoinTable = (Table) firstJoin.getFromItem();
        assertEquals("departments", firstJoinTable.getName());
        
        // Second join
        Join secondJoin = joins.get(1);
        assertTrue(secondJoin.isLeft());
        Table secondJoinTable = (Table) secondJoin.getFromItem();
        assertEquals("projects", secondJoinTable.getName());
    }

    @Test
    void testSelectWithSubquery() throws Exception {
        String sql = "SELECT id, name FROM users " +
                     "WHERE salary > (SELECT AVG(salary) FROM employees)";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        // Check WHERE contains subquery
        Expression where = plainSelect.getWhere();
        assertNotNull(where);
        assertTrue(where.toString().contains("SELECT"));
        assertTrue(where.toString().contains("AVG(salary)"));
    }

    @Test
    void testInsertStatement() throws Exception {
        String sql = "INSERT INTO users (id, name, email) VALUES (1, 'John', 'john@example.com')";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(Insert.class, statement);
        
        Insert insert = (Insert) statement;
        assertEquals("users", insert.getTable().getName());
        
        List<Column> columns = insert.getColumns();
        assertNotNull(columns);
        assertEquals(3, columns.size());
        assertEquals("id", columns.get(0).getColumnName());
        assertEquals("name", columns.get(1).getColumnName());
        assertEquals("email", columns.get(2).getColumnName());
    }

    @Test
    void testUpdateStatement() throws Exception {
        String sql = "UPDATE users SET name = 'Jane', status = 'active' WHERE id = 123";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(Update.class, statement);
        
        Update update = (Update) statement;
        assertEquals("users", update.getTable().getName());
        
        // Check WHERE clause
        assertNotNull(update.getWhere());
        assertTrue(update.getWhere().toString().contains("id = 123"));
    }

    @Test
    void testDeleteStatement() throws Exception {
        String sql = "DELETE FROM users WHERE status = 'inactive' AND last_login < '2023-01-01'";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(Delete.class, statement);
        
        Delete delete = (Delete) statement;
        assertEquals("users", delete.getTable().getName());
        
        // Check WHERE clause
        assertNotNull(delete.getWhere());
        String whereStr = delete.getWhere().toString();
        assertTrue(whereStr.contains("status = 'inactive'"));
        assertTrue(whereStr.contains("last_login < '2023-01-01'"));
    }

    @Test
    void testTableNamesExtraction() throws Exception {
        String sql = "SELECT u.name, o.total FROM users u " +
                     "JOIN orders o ON u.id = o.user_id " +
                     "WHERE u.created_at > '2024-01-01'";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableNames = tablesNamesFinder.getTableList(statement);
        
        assertEquals(2, tableNames.size());
        assertTrue(tableNames.contains("users"));
        assertTrue(tableNames.contains("orders"));
    }

    @Test
    void testComplexSelectWithGroupByAndHaving() throws Exception {
        String sql = "SELECT dept_id, COUNT(*) as emp_count, AVG(salary) as avg_salary " +
                     "FROM employees " +
                     "GROUP BY dept_id " +
                     "HAVING COUNT(*) > 5 " +
                     "ORDER BY avg_salary DESC";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        // Check GROUP BY
        GroupByElement groupBy = plainSelect.getGroupBy();
        assertNotNull(groupBy);
        assertFalse(groupBy.getGroupByExpressionList().isEmpty());
        
        // Check HAVING
        Expression having = plainSelect.getHaving();
        assertNotNull(having);
        assertTrue(having.toString().contains("COUNT(*) > 5"));
        
        // Check ORDER BY
        List<OrderByElement> orderBy = plainSelect.getOrderByElements();
        assertNotNull(orderBy);
        assertEquals(1, orderBy.size());
    }

    @Test
    void testSelectDistinct() throws Exception {
        String sql = "SELECT DISTINCT country, city FROM customers";
        
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;
        PlainSelect plainSelect = select.getPlainSelect();
        
        // Check DISTINCT
        assertNotNull(plainSelect.getDistinct());
    }

    @Test
    void testInvalidSqlThrowsException() {
        String invalidSql = "SELECT * FROM WHERE id = 1";
        
        assertThrows(Exception.class, () -> {
            CCJSqlParserUtil.parse(invalidSql);
        });
    }
}
