-- Seed example entitlement policies for testing
-- These demonstrate RLS, CLS, MASK, and TABLE_ACCESS policies

-- First, ensure we have a demo tenant
INSERT INTO tenants (tenant_id, name, created_at, updated_at)
VALUES ('demo-tenant', 'Demo Tenant Organization', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = EXCLUDED.updated_at;

-- Policy 1: RLS - Users can only see their own assigned GitHub issues
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'rls-github-own-issues',
    'Users see only their assigned GitHub issues',
    'RLS',
    'github',
    'issues',
    'user.role != ''ADMIN''',
    'FILTER',
    '{"filter_type": "row", "column": "assignee", "operator": "=", "value": "$${user.id}"}',
    10
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 2: RLS - Users can only see Jira issues from their allowed projects
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'rls-jira-project-access',
    'Users see only issues from their allowed projects',
    'RLS',
    'jira',
    'issues',
    NULL,
    'FILTER',
    '{"filter_type": "row", "column": "project", "operator": "IN", "value": "$${user.allowed_projects}"}',
    10
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 3: RLS - Draft PRs only visible to their authors
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'rls-github-draft-prs',
    'Draft PRs only visible to author',
    'RLS',
    'github',
    'pulls',
    'user.role != ''ADMIN''',
    'FILTER',
    '{"filter_type": "complex", "logic": "OR", "filters": [
        {"column": "draft", "operator": "=", "value": false},
        {"column": "author", "operator": "=", "value": "$${user.id}"}
    ]}',
    10
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 4: CLS - Hide email from Jira users for non-HR
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'cls-jira-hide-email',
    'Hide email from non-HR users',
    'CLS',
    'jira',
    'users',
    'user.role != ''HR_ADMIN''',
    'DENY',
    '{"denied_columns": ["email"]}',
    10
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 5: MASK - Mask sensitive columns for external users
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'mask-github-private-repos',
    'Mask private repository names for external users',
    'MASK',
    'github',
    'repositories',
    'user.type = ''EXTERNAL''',
    'MASK',
    '{"column": "full_name", "mask_type": "PARTIAL",
      "condition_column": "private", "condition_value": true}',
    10
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 6: TABLE_ACCESS - Deny access to audit logs
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'deny-audit-logs',
    'Restrict audit log access to security admins',
    'TABLE_ACCESS',
    '*',
    'audit_logs',
    'user.role != ''SECURITY_ADMIN''',
    'DENY',
    '{}',
    100
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 7: High Priority - Admins see everything
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'admin-all-access',
    'Admins see all data',
    'TABLE_ACCESS',
    '*',
    '*',
    'user.role = ''ADMIN''',
    'ALLOW',
    '{}',
    1000
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;

-- Policy 8: RLS - Region-based access for Jira
INSERT INTO entitlement_policies (
    tenant_id, policy_id, policy_name, policy_type,
    source_pattern, table_pattern, condition, action, policy_config, priority
) VALUES (
    'demo-tenant',
    'rls-jira-region',
    'Users see only issues from their region',
    'RLS',
    'jira',
    'issues',
    'user.region IS NOT NULL',
    'FILTER',
    '{"filter_type": "row", "column": "project", "operator": "IN",
      "value": "$${user.regional_projects}"}',
    20
)
ON CONFLICT (policy_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    policy_name = EXCLUDED.policy_name,
    policy_type = EXCLUDED.policy_type,
    source_pattern = EXCLUDED.source_pattern,
    table_pattern = EXCLUDED.table_pattern,
    condition = EXCLUDED.condition,
    action = EXCLUDED.action,
    policy_config = EXCLUDED.policy_config,
    priority = EXCLUDED.priority,
    updated_at = CURRENT_TIMESTAMP;
