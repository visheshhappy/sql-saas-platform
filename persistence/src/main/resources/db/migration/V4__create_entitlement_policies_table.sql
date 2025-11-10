-- Create entitlement_policies table
-- Stores tenant-specific access control policies for row-level security (RLS),
-- column-level security (CLS), data masking, and access denial

CREATE TABLE entitlement_policies (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR NOT NULL,          -- matches tenants.tenant_id
    policy_id VARCHAR(100) UNIQUE NOT NULL,
    policy_name VARCHAR(255),

    -- What type of restriction?
    policy_type VARCHAR(50) NOT NULL,    -- 'RLS', 'CLS', 'MASK', 'TABLE_ACCESS'

    -- Which connector and resource does this apply to?
    source_pattern VARCHAR(255),         -- 'github', 'jira', '*' (all)
    table_pattern VARCHAR(255),          -- 'issues', 'pulls', '*' (all tables)

    -- When should this policy apply?
    condition TEXT,                      -- Expression: "user.role != 'ADMIN'"

    -- What should happen?
    action VARCHAR(50),                  -- 'ALLOW', 'DENY', 'FILTER', 'MASK'

    -- Configuration details (JSON)
    policy_config JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Metadata
    priority INT DEFAULT 0,              -- Higher priority = evaluated first
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT fk_entitlement_policies_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX idx_entitlement_policies_tenant
    ON entitlement_policies(tenant_id);

CREATE INDEX idx_entitlement_policies_source
    ON entitlement_policies(source_pattern, table_pattern);

CREATE INDEX idx_entitlement_policies_enabled
    ON entitlement_policies(enabled);

CREATE INDEX idx_entitlement_policies_priority
    ON entitlement_policies(priority);