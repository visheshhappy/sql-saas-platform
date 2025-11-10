-- Initialize SQL SaaS Platform Database Schema
-- This script runs when the PostgreSQL container is first created

-- Create necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE sqlsaasdb TO sqlsaas;

-- Note: Flyway will handle the actual schema creation via migrations
