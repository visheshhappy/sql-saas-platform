package com.thp.sqlsaas.connector;

/**
 * Types of connectors supported by the platform.
 */
public enum ConnectorType {
    GITHUB("github", "GitHub"),
    JIRA("jira", "Jira"),
    SALESFORCE("salesforce", "Salesforce"),
    ZENDESK("zendesk", "Zendesk"),
    SLACK("slack", "Slack"),
    NOTION("notion", "Notion");
    
    private final String id;
    private final String displayName;
    
    ConnectorType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static ConnectorType fromId(String id) {
        for (ConnectorType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown connector type: " + id);
    }
}
