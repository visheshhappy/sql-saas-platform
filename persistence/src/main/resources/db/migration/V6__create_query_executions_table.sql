-- Create query_executions table for tracking query execution state
CREATE TABLE IF NOT EXISTS query_executions (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(255) UNIQUE NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    sql_query TEXT,
    connector_type VARCHAR(100),
    resource VARCHAR(255),
    state VARCHAR(50) NOT NULL,
    status VARCHAR(50),
    error_code VARCHAR(100),
    error_message TEXT,
    rows_returned INTEGER,
    execution_time_ms BIGINT,
    freshness_ms BIGINT,
    cache_hit BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_query_executions_trace_id ON query_executions(trace_id);
CREATE INDEX idx_query_executions_tenant_id ON query_executions(tenant_id, created_at DESC);
CREATE INDEX idx_query_executions_user_id ON query_executions(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_query_executions_state ON query_executions(state);
CREATE INDEX idx_query_executions_created_at ON query_executions(created_at DESC);
