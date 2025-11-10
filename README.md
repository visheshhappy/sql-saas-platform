# Universal SQL SaaS Platform - Quick Start Guide

## Overview

## Prerequisites

- **Java 17+** (Amazon Corretto or OpenJDK)
- **Docker & Docker Compose** (for PostgreSQL)
- **Gradle 8.5+** (wrapper included)

## Quick Start (5 minutes)

### 1. Start PostgreSQL Database

```bash
# Start PostgreSQL container
docker-compose up -d postgres

# Verify database is running
docker ps
```

### 2. Run the Application

```bash
# Build and start server (includes Flyway migrations)
./gradlew :server:bootRun

# Server starts on: http://localhost:9090
```

### 3. Execute Your First Query

```bash
curl -X POST http://localhost:9090/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM github_issues WHERE state = '\''open'\''",
    "tenantId": "tenant-123",
    "userId": "user-456",
    "userRoles": ["developer"],
    "maxStalenessMs": 60000
  }'
```

**Expected Response:**
```json
{
  "status": "SUCCESS",
  "rows": [
    {
      "id": "issue-1",
      "number": 42,
      "title": "Fix authentication bug",
      "state": "open",
      "assignee": "user-456"
    }
  ],
  "freshnessMs": 50,
  "rateLimitStatus": "OK",
  "traceId": "abc-123-def",
  "executionTimeMs": 85
}
```

## Project Structure

```
sql-saas-platform/
├── server/              # REST API & orchestration
├── connector/           # GitHub & Jira mock connectors
├── entitlement/         # Security policies (RLS/CLS/MASK)
├── persistence/         # Database entities & Flyway migrations
├── sqlparser/           # SQL parsing logic
└── core/                # Shared models
```

## Key Components

| Component | Purpose | Port |
|-----------|---------|------|
| **Server** | REST API Gateway | 9090 |
| **PostgreSQL** | Metadata & state storage | 5432 |
| **Query Orchestrator** | Central coordinator | - |
| **EntitlementService** | Row/Column security | - |
| **RateLimitService** | Token bucket limiting | - |
| **Connectors** | GitHub/Jira mock data | - |

## Available SQL Queries

### GitHub Connector
```sql
-- Query issues
SELECT * FROM github_issues WHERE state = 'open';
SELECT * FROM github_issues WHERE assignee = 'alice';

-- Query pull requests
SELECT * FROM github_pulls WHERE state = 'open';
```

### Jira Connector
```sql
-- Query issues
SELECT * FROM jira_issues WHERE status = 'In Progress';
SELECT * FROM jira_issues WHERE project = 'PROJ1';

-- Query projects
SELECT * FROM jira_projects;
```

## Database Access

**PostgreSQL** 
```bash
docker exec -it sql-saas-platform_postgres_1 psql -U sqlsaas -d sqlsaasdb

# View policies
SELECT policy_id, policy_type, source_pattern FROM entitlement_policies;

# View query executions
SELECT trace_id, state, rows_returned FROM query_executions ORDER BY created_at DESC LIMIT 10;
```

## Configuration

**Application Properties** (`server/src/main/resources/application.properties`):
```properties
server.port=9090
spring.datasource.url=jdbc:postgresql://localhost:5432/sqlsaasdb
spring.datasource.username=sqlsaas
spring.datasource.password=secret
```

## Security Features

### 1. Row-Level Security (RLS)
Users only see their own data based on policies:
```
Policy: "user.role != 'ADMIN'" → Filter: assignee = user.id
Result: User sees only issues assigned to them
```

### 2. Column-Level Security (CLS)
Hide sensitive columns from non-admins:
```
Denied columns: internal_notes, salary_info, ssn
```

### 3. Data Masking
Partially mask sensitive data:
```
Email: john@example.com → joh@*****.com (PARTIAL)
Phone: 555-1234 → **** (FULL)
```

### 4. Rate Limiting
Token bucket algorithm:
```
Capacity: 100 requests
Refill: 10 requests/second
```

## Common Commands

```bash
# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Clean build
./gradlew clean build

# Skip tests
./gradlew build -x test

# Run with specific profile
./gradlew :server:bootRun --args='--spring.profiles.active=prod'
```

## Troubleshooting

**Issue:** Port 9090 already in use
```bash
lsof -ti:9090 | xargs kill -9
```

**Issue:** Database connection failed
```bash
docker-compose restart postgres
./gradlew :server:bootRun
```

**Issue:** Flyway migration failed
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d postgres
./gradlew :server:bootRun
```

## Architecture Highlights

- **Multi-Module Gradle** - Clean separation of concerns
- **Spring Boot 3** - Modern Java framework
- **Flyway** - Database version control
- **JPA/Hibernate** - ORM with PostgreSQL
- **JSqlParser** - SQL parsing library
- **Mock Connectors** - GitHub & Jira test data

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/query` | Execute SQL query |
| GET | `/health` | Health check |
