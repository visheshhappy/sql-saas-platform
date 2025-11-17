-- Create databases table to track tenant databases
CREATE TABLE tenant_databases (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    database_name VARCHAR(255) NOT NULL,
    database_type VARCHAR(50) NOT NULL,
    connection_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_database UNIQUE (tenant_id, database_name)
);

-- Create index for faster tenant lookups
CREATE INDEX idx_tenant_databases_tenant_id ON tenant_databases(tenant_id);
CREATE INDEX idx_tenant_databases_status ON tenant_databases(status);
