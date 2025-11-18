-- Migration: Add user roles tables
-- This adds proper authentication/authorization support
-- Roles are now stored in database (source of truth) instead of client input

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_user_id ON users(user_id);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- User-Role assignments
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    granted_at TIMESTAMP DEFAULT NOW(),
    granted_by VARCHAR(100),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role_tenant UNIQUE (user_id, role_id, tenant_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Seed default roles
INSERT INTO roles (role_name, description) VALUES
    ('admin', 'Full administrative access to all resources'),
    ('developer', 'Developer access to code and issues'),
    ('viewer', 'Read-only access to resources'),
    ('data_analyst', 'Access to analytics and reporting')
ON CONFLICT (role_name) DO NOTHING;

-- Sample users for demo tenant (for testing)
INSERT INTO users (user_id, tenant_id, email, name) VALUES
    ('john_doe', 'demo-tenant', 'john.doe@demo.com', 'John Doe'),
    ('jane_smith', 'demo-tenant', 'jane.smith@demo.com', 'Jane Smith'),
    ('admin_user', 'demo-tenant', 'admin@demo.com', 'Admin User'),
    ('viewer_user', 'demo-tenant', 'viewer@demo.com', 'Viewer User')
ON CONFLICT (tenant_id, user_id) DO NOTHING;

-- Assign roles to sample users
INSERT INTO user_roles (user_id, role_id, tenant_id, granted_by)
SELECT
    u.id,
    r.id,
    u.tenant_id,
    'system'
FROM users u
CROSS JOIN roles r
WHERE (u.user_id = 'john_doe' AND r.role_name = 'developer')
   OR (u.user_id = 'jane_smith' AND r.role_name IN ('developer', 'data_analyst'))
   OR (u.user_id = 'admin_user' AND r.role_name = 'admin')
   OR (u.user_id = 'viewer_user' AND r.role_name = 'viewer')
ON CONFLICT (user_id, role_id, tenant_id) DO NOTHING;