package com.thp.sqlsaas.connector.impl;

import com.thp.sqlsaas.connector.BaseConnector;
import com.thp.sqlsaas.connector.Connector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mock connector for Jira.
 * Simulates Jira Issues, Projects, and Users data.
 * 
 * Supported resources:
 * - issues: Jira issues/tickets
 * - projects: Jira projects
 * - users: Jira users
 */
public class JiraMockConnector extends BaseConnector {
    
    private static final String CONNECTOR_ID = "jira";
    private static final String DISPLAY_NAME = "Jira";
    
    // Mock data storage
    private Map<String, List<Map<String, Object>>> mockData;
    
    public JiraMockConnector() {
        this.mockData = new HashMap<>();
    }
    
    @Override
    public String getConnectorId() {
        return CONNECTOR_ID;
    }
    
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
    
    @Override
    protected Map<String, Object> performConnect(ConnectRequest req) throws Exception {
        // Initialize mock data
        initializeMockData();
        
        // In a real implementation, this would:
        // 1. Validate API token or OAuth credentials
        // 2. Test connection to Jira instance
        // 3. Retrieve user permissions
        
        Map<String, Object> context = new HashMap<>();
        context.put("connected_at", LocalDateTime.now().toString());
        context.put("instance_url", "https://mock-company.atlassian.net");
        context.put("user", "mock-jira-user");
        context.put("permissions", Arrays.asList("BROWSE_PROJECTS", "CREATE_ISSUES", "EDIT_ISSUES"));
        
        return context;
    }
    
    @Override
    protected CapabilityDescriptor buildCapabilities() {
        // Define what resources and columns this connector supports
        Set<String> resources = Set.of("issues", "projects", "users");
        
        Map<String, Set<String>> columns = new HashMap<>();
        columns.put("issues", Set.of(
            "id", "key", "summary", "description", "status", "priority", 
            "issue_type", "project", "assignee", "reporter", "created_at", 
            "updated_at", "resolved_at", "labels", "story_points", "sprint"
        ));
        columns.put("projects", Set.of(
            "id", "key", "name", "description", "lead", "category",
            "created_at", "updated_at"
        ));
        columns.put("users", Set.of(
            "id", "email", "display_name", "account_type", "active"
        ));
        
        // Define which fields support predicate pushdown
        Map<String, Set<String>> pushdownableFields = new HashMap<>();
        pushdownableFields.put("issues", Set.of("status", "project", "assignee", "priority", "issue_type"));
        pushdownableFields.put("projects", Set.of("category", "lead"));
        pushdownableFields.put("users", Set.of("active", "account_type"));
        
        return new CapabilityDescriptor(resources, columns, pushdownableFields);
    }
    
    @Override
    protected List<String> getAllowedResources(ConnectRequest req) {
        // In a real implementation, this would query Jira API for
        // projects the user has access to
        return Arrays.asList("PROJ1", "PROJ2", "PROJ3");
    }
    
    @Override
    protected List<Map<String, Object>> fetchAllRows(String resource) throws Exception {
        List<Map<String, Object>> rows = mockData.get(resource);
        if (rows == null) {
            throw new IllegalArgumentException("Unknown resource: " + resource);
        }
        // Return a copy to avoid external modification
        return new ArrayList<>(rows);
    }
    
    /**
     * Initialize mock data for testing.
     */
    private void initializeMockData() {
        mockData = new HashMap<>();
        
        // Mock Issues
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.add(createIssue("PROJ1-101", "PROJ1", "Bug", "Login page not responsive", 
            "In Progress", "High", "john_doe", "jane_smith", 5, "Sprint 23"));
        issues.add(createIssue("PROJ1-102", "PROJ1", "Story", "Implement dark mode", 
            "To Do", "Medium", "jane_smith", "jane_smith", 8, "Sprint 24"));
        issues.add(createIssue("PROJ1-103", "PROJ1", "Task", "Update API documentation", 
            "Done", "Low", "bob_jones", "john_doe", 3, "Sprint 22"));
        issues.add(createIssue("PROJ2-201", "PROJ2", "Bug", "Database connection timeout", 
            "In Progress", "Critical", "john_doe", "alice_admin", 8, "Sprint 23"));
        issues.add(createIssue("PROJ2-202", "PROJ2", "Epic", "Microservices migration", 
            "To Do", "High", null, "alice_admin", 21, "Sprint 25"));
        issues.add(createIssue("PROJ2-203", "PROJ2", "Story", "User profile redesign", 
            "In Progress", "Medium", "jane_smith", "bob_jones", 13, "Sprint 23"));
        issues.add(createIssue("PROJ3-301", "PROJ3", "Bug", "Payment gateway integration failing", 
            "Done", "Critical", "bob_jones", "john_doe", 5, "Sprint 22"));
        issues.add(createIssue("PROJ3-302", "PROJ3", "Task", "Add unit tests for payment module", 
            "In Progress", "Medium", "john_doe", "bob_jones", 5, "Sprint 23"));
        issues.add(createIssue("PROJ1-104", "PROJ1", "Story", "Implement 2FA authentication", 
            "To Do", "High", "jane_smith", "alice_admin", 13, "Sprint 24"));
        issues.add(createIssue("PROJ2-204", "PROJ2", "Bug", "Memory leak in background jobs", 
            "In Progress", "High", "john_doe", "alice_admin", 8, "Sprint 23"));
        mockData.put("issues", issues);
        
        // Mock Projects
        List<Map<String, Object>> projects = new ArrayList<>();
        projects.add(createProject("PROJ1", "Project Alpha", 
            "Main product development", "alice_admin", "Software"));
        projects.add(createProject("PROJ2", "Project Beta", 
            "Infrastructure and DevOps", "john_doe", "IT"));
        projects.add(createProject("PROJ3", "Project Gamma", 
            "Payment processing system", "bob_jones", "Business"));
        mockData.put("projects", projects);
        
        // Mock Users
        List<Map<String, Object>> users = new ArrayList<>();
        users.add(createUser("john_doe", "john.doe@company.com", "John Doe", "atlassian", true));
        users.add(createUser("jane_smith", "jane.smith@company.com", "Jane Smith", "atlassian", true));
        users.add(createUser("bob_jones", "bob.jones@company.com", "Bob Jones", "atlassian", true));
        users.add(createUser("alice_admin", "alice.admin@company.com", "Alice Admin", "atlassian", true));
        users.add(createUser("old_user", "old.user@company.com", "Old User", "atlassian", false));
        mockData.put("users", users);
    }
    
    private Map<String, Object> createIssue(
            String key, String project, String issueType, String summary,
            String status, String priority, String assignee, String reporter,
            int storyPoints, String sprint) {
        
        Map<String, Object> issue = new HashMap<>();
        issue.put("id", key.replace("-", "_"));
        issue.put("key", key);
        issue.put("project", project);
        issue.put("issue_type", issueType);
        issue.put("summary", summary);
        issue.put("description", "Detailed description for " + summary);
        issue.put("status", status);
        issue.put("priority", priority);
        issue.put("assignee", assignee);
        issue.put("reporter", reporter);
        issue.put("story_points", storyPoints);
        issue.put("sprint", sprint);
        issue.put("labels", generateLabels(issueType, priority));
        issue.put("created_at", getCurrentTimestamp(-30));
        issue.put("updated_at", getCurrentTimestamp(-2));
        issue.put("resolved_at", status.equals("Done") ? getCurrentTimestamp(-1) : null);
        
        return issue;
    }
    
    private List<String> generateLabels(String issueType, String priority) {
        List<String> labels = new ArrayList<>();
        labels.add(issueType.toLowerCase());
        if (priority.equals("Critical") || priority.equals("High")) {
            labels.add("urgent");
        }
        return labels;
    }
    
    private Map<String, Object> createProject(
            String key, String name, String description, String lead, String category) {
        
        Map<String, Object> project = new HashMap<>();
        project.put("id", "project_" + key.toLowerCase());
        project.put("key", key);
        project.put("name", name);
        project.put("description", description);
        project.put("lead", lead);
        project.put("category", category);
        project.put("created_at", getCurrentTimestamp(-180));
        project.put("updated_at", getCurrentTimestamp(-1));
        
        return project;
    }
    
    private Map<String, Object> createUser(
            String id, String email, String displayName, String accountType, boolean active) {
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("email", email);
        user.put("display_name", displayName);
        user.put("account_type", accountType);
        user.put("active", active);
        
        return user;
    }
    
    private String getCurrentTimestamp(int daysOffset) {
        LocalDateTime time = LocalDateTime.now().plusDays(daysOffset);
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
