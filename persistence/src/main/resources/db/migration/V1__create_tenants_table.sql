-- Create tenants table
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_tenant_id UNIQUE (tenant_id)
);

-- Create index on tenant_id for faster lookups
CREATE INDEX idx_tenants_tenant_id ON tenants(tenant_id);

-- Insert a sample tenant
INSERT INTO tenants (tenant_id, name, created_at, updated_at) 
VALUES ('default-tenant', 'Default Tenant', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
