package com.thp.sqlsaas.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing an entitlement policy for access control.
 * Supports RLS (Row-Level Security), CLS (Column-Level Security), 
 * MASK (Data Masking), and TABLE_ACCESS (Deny/Allow).
 */
@Entity
@Table(name = "entitlement_policies")
public class EntitlementPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "policy_id", nullable = false, unique = true, length = 100)
    private String policyId;

    @Column(name = "policy_name", length = 255)
    private String policyName;

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType; // RLS, CLS, MASK, TABLE_ACCESS

    @Column(name = "source_pattern", length = 255)
    private String sourcePattern; // github, jira, *

    @Column(name = "table_pattern", length = 255)
    private String tablePattern; // issues, pulls, *

    @Column(name = "condition", columnDefinition = "TEXT")
    private String condition; // user.role != 'ADMIN'

    @Column(name = "action", length = 50)
    private String action; // ALLOW, DENY, FILTER, MASK

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_config", columnDefinition = "jsonb")
    private Map<String, Object> policyConfig;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public EntitlementPolicy() {
    }

    public EntitlementPolicy(String tenantId, String policyId, String policyName, 
                           String policyType, String sourcePattern, String tablePattern) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyType = policyType;
        this.sourcePattern = sourcePattern;
        this.tablePattern = tablePattern;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    public String getTablePattern() {
        return tablePattern;
    }

    public void setTablePattern(String tablePattern) {
        this.tablePattern = tablePattern;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(Map<String, Object> policyConfig) {
        this.policyConfig = policyConfig;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Check if policy matches the given source and table
     */
    public boolean matches(String sourceId, String tableName) {
        boolean sourceMatch = matchesPattern(sourceId, sourcePattern);
        boolean tableMatch = matchesPattern(tableName, tablePattern);
        return sourceMatch && tableMatch;
    }

    private boolean matchesPattern(String value, String pattern) {
        if (pattern == null || pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return value.startsWith(prefix);
        }
        return pattern.equals(value);
    }

    @Override
    public String toString() {
        return "EntitlementPolicy{" +
                "id=" + id +
                ", policyId='" + policyId + '\'' +
                ", policyType='" + policyType + '\'' +
                ", sourcePattern='" + sourcePattern + '\'' +
                ", tablePattern='" + tablePattern + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
