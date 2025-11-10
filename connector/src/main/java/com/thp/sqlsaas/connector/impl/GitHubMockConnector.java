package com.thp.sqlsaas.connector.impl;

import com.thp.sqlsaas.connector.BaseConnector;
import com.thp.sqlsaas.connector.Connector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mock connector for GitHub.
 * Simulates GitHub Issues and Pull Requests data.
 * 
 * Supported resources:
 * - issues: GitHub issues
 * - pulls: GitHub pull requests
 * - repositories: GitHub repositories
 */
public class GitHubMockConnector extends BaseConnector {
    
    private static final String CONNECTOR_ID = "github";
    private static final String DISPLAY_NAME = "GitHub";
    
    // Mock data storage
    private Map<String, List<Map<String, Object>>> mockData;
    
    public GitHubMockConnector() {
        this.mockData = new HashMap<>();
        // Initialize mock data immediately so it's available for tests
        initializeMockData();
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
        // 1. Validate OAuth token
        // 2. Exchange token for API credentials
        // 3. Test connection to GitHub API
        
        Map<String, Object> context = new HashMap<>();
        context.put("connected_at", LocalDateTime.now().toString());
        context.put("user", "mock-user");
        context.put("scopes", Arrays.asList("repo", "read:org", "read:user"));
        
        return context;
    }
    
    @Override
    protected CapabilityDescriptor buildCapabilities() {
        // Define what resources and columns this connector supports
        Set<String> resources = Set.of("issues", "pulls", "repositories");
        
        Map<String, Set<String>> columns = new HashMap<>();
        columns.put("issues", Set.of(
            "id", "number", "title", "state", "labels", "assignee", 
            "created_at", "updated_at", "closed_at", "body", "repository", "author"
        ));
        columns.put("pulls", Set.of(
            "id", "number", "title", "state", "created_at", "updated_at", 
            "merged_at", "head_ref", "base_ref", "repository", "author", "draft"
        ));
        columns.put("repositories", Set.of(
            "id", "name", "full_name", "description", "private", "language",
            "stargazers_count", "forks_count", "created_at", "updated_at"
        ));
        
        // Define which fields support predicate pushdown
        Map<String, Set<String>> pushdownableFields = new HashMap<>();
        pushdownableFields.put("issues", Set.of("state", "repository", "assignee", "labels"));
        pushdownableFields.put("pulls", Set.of("state", "repository", "draft"));
        pushdownableFields.put("repositories", Set.of("language", "private"));
        
        return new CapabilityDescriptor(resources, columns, pushdownableFields);
    }
    
    @Override
    protected List<String> getAllowedResources(ConnectRequest req) {
        // In a real implementation, this would query GitHub API for
        // repositories the user has access to
        return Arrays.asList(
            "org/repo1",
            "org/repo2",
            "user/personal-project"
        );
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
        issues.add(createIssue(1, "org/repo1", "Bug in login page", "open", "bug", "john_doe"));
        issues.add(createIssue(2, "org/repo1", "Add dark mode", "open", "enhancement", "jane_smith"));
        issues.add(createIssue(3, "org/repo2", "Update documentation", "closed", "documentation", "bob_jones"));
        issues.add(createIssue(4, "org/repo1", "Performance issue", "open", "bug", "john_doe"));
        issues.add(createIssue(5, "user/personal-project", "Refactor codebase", "open", "refactoring", "john_doe"));
        issues.add(createIssue(6, "org/repo2", "Security vulnerability", "closed", "security", "jane_smith"));
        issues.add(createIssue(7, "org/repo1", "Feature request: API v2", "open", "enhancement", null));
        issues.add(createIssue(8, "org/repo2", "CI/CD pipeline failing", "open", "ci", "bob_jones"));
        mockData.put("issues", issues);
        
        // Mock Pull Requests
        List<Map<String, Object>> pulls = new ArrayList<>();
        pulls.add(createPullRequest(101, "org/repo1", "Fix login bug", "open", "fix/login-bug", "main", false, "john_doe"));
        pulls.add(createPullRequest(102, "org/repo1", "Add dark mode UI", "open", "feature/dark-mode", "main", false, "jane_smith"));
        pulls.add(createPullRequest(103, "org/repo2", "Update README", "merged", "docs/update-readme", "main", false, "bob_jones"));
        pulls.add(createPullRequest(104, "org/repo1", "WIP: Refactoring", "open", "refactor/cleanup", "main", true, "john_doe"));
        pulls.add(createPullRequest(105, "org/repo2", "Security patch", "merged", "security/patch-cve", "main", false, "jane_smith"));
        pulls.add(createPullRequest(106, "user/personal-project", "Improve performance", "open", "perf/optimization", "develop", false, "john_doe"));
        mockData.put("pulls", pulls);
        
        // Mock Repositories
        List<Map<String, Object>> repos = new ArrayList<>();
        repos.add(createRepository(1001, "repo1", "org/repo1", "Main application repository", false, "Java", 145, 23));
        repos.add(createRepository(1002, "repo2", "org/repo2", "Documentation site", false, "Python", 89, 12));
        repos.add(createRepository(1003, "personal-project", "user/personal-project", "Personal experiments", true, "JavaScript", 5, 0));
        mockData.put("repositories", repos);
    }
    
    private Map<String, Object> createIssue(
            int number, String repo, String title, String state, 
            String label, String assignee) {
        
        Map<String, Object> issue = new HashMap<>();
        issue.put("id", "issue_" + number);
        issue.put("number", number);
        issue.put("repository", repo);
        issue.put("title", title);
        issue.put("state", state);
        issue.put("labels", Arrays.asList(label));
        issue.put("assignee", assignee);
        issue.put("author", assignee != null ? assignee : "unknown_user");
        issue.put("body", "This is the body of issue #" + number);
        issue.put("created_at", getCurrentTimestamp(-30));
        issue.put("updated_at", getCurrentTimestamp(-5));
        issue.put("closed_at", state.equals("closed") ? getCurrentTimestamp(-1) : null);
        
        return issue;
    }
    
    private Map<String, Object> createPullRequest(
            int number, String repo, String title, String state,
            String headRef, String baseRef, boolean draft, String author) {
        
        Map<String, Object> pr = new HashMap<>();
        pr.put("id", "pr_" + number);
        pr.put("number", number);
        pr.put("repository", repo);
        pr.put("title", title);
        pr.put("state", state);
        pr.put("head_ref", headRef);
        pr.put("base_ref", baseRef);
        pr.put("draft", draft);
        pr.put("author", author);
        pr.put("created_at", getCurrentTimestamp(-20));
        pr.put("updated_at", getCurrentTimestamp(-3));
        pr.put("merged_at", state.equals("merged") ? getCurrentTimestamp(-1) : null);
        
        return pr;
    }
    
    private Map<String, Object> createRepository(
            int id, String name, String fullName, String description,
            boolean isPrivate, String language, int stars, int forks) {
        
        Map<String, Object> repo = new HashMap<>();
        repo.put("id", "repo_" + id);
        repo.put("name", name);
        repo.put("full_name", fullName);
        repo.put("description", description);
        repo.put("private", isPrivate);
        repo.put("language", language);
        repo.put("stargazers_count", stars);
        repo.put("forks_count", forks);
        repo.put("created_at", getCurrentTimestamp(-365));
        repo.put("updated_at", getCurrentTimestamp(-2));
        
        return repo;
    }
    
    private String getCurrentTimestamp(int daysOffset) {
        LocalDateTime time = LocalDateTime.now().plusDays(daysOffset);
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
