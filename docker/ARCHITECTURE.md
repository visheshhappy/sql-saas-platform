# Docker Architecture for SQL SaaS Platform

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Host Machine                                 │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │           sql-saas-network (Bridge Network)                │   │
│  │                                                            │   │
│  │  ┌──────────────────────────────────────────────────┐    │   │
│  │  │          SQL SaaS Server                          │    │   │
│  │  │                                                  │    │   │
│  │  │  - Spring Boot Application                       │    │   │
│  │  │  - Port: 9090                                    │    │   │
│  │  │  - JVM: -Xms512M -Xmx1024M                      │    │   │
│  │  │  - Profile: docker                               │    │   │
│  │  │  - Health: /actuator/health                      │    │   │
│  │  │                                                  │    │   │
│  │  └──────────┬───────────┬──────────┬────────────────┘    │   │
│  │             │           │          │                      │   │
│  │             │           │          │                      │   │
│  │     ┌───────▼──┐  ┌────▼────┐  ┌──▼──────────┐         │   │
│  │     │PostgreSQL│  │  Redis  │  │    Kafka    │         │   │
│  │     │          │  │         │  │             │         │   │
│  │     │Port:5432 │  │Port:6379│  │Port:29092   │         │   │
│  │     │DB:       │  │AOF:yes  │  │Topics:auto  │         │   │
│  │     │sqlsaasdb │  │         │  │             │         │   │
│  │     │          │  │         │  │             │         │   │
│  │     └──────────┘  └─────────┘  └──────┬──────┘         │   │
│  │                                        │                 │   │
│  │                                  ┌─────▼────────┐       │   │
│  │                                  │  Zookeeper   │       │   │
│  │                                  │              │       │   │
│  │                                  │  Port:2181   │       │   │
│  │                                  └──────────────┘       │   │
│  │                                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  Port Mappings (Host → Container):                              │
│  ├─ 5434 → postgres:5432 (CUSTOM PORT)                         │
│  ├─ 6380 → redis:6379 (CUSTOM PORT)                            │
│  ├─ 9092 → kafka:9092                                          │
│  └─ 9090 → sql-saas-server:9090                                │
│                                                                  │
│  Persistent Volumes:                                             │
│  ├─ postgres_data  → /var/lib/postgresql/data                  │
│  ├─ redis_data     → /data                                     │
│  ├─ kafka_data     → /var/lib/kafka/data                       │
│  └─ zookeeper_data → /var/lib/zookeeper/data                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## Component Details

### SQL SaaS Server
- **Image:** Custom build from Dockerfile
- **Base:** Eclipse Temurin 17 JRE
- **Port:** 9090
- **Dependencies:** PostgreSQL, Redis, Kafka
- **Health Check:** HTTP GET /actuator/health every 30s

### PostgreSQL
- **Image:** postgres:15-alpine
- **Port:** 5434 (host) → 5432 (container)
- **Database:** sqlsaasdb
- **User:** sqlsaas / sqlsaas123
- **Volume:** postgres_data
- **Health Check:** pg_isready every 10s

### Redis
- **Image:** redis:7-alpine
- **Port:** 6380 (host) → 6379 (container)
- **Persistence:** AOF (Append Only File)
- **Volume:** redis_data
- **Health Check:** PING every 10s

### Kafka
- **Image:** confluentinc/cp-kafka:7.5.0
- **Port:** 9092
- **Internal Port:** 29092
- **Dependencies:** Zookeeper
- **Volume:** kafka_data
- **Health Check:** broker-api-versions every 10s

### Zookeeper
- **Image:** confluentinc/cp-zookeeper:7.5.0
- **Port:** 2181 (internal only)
- **Purpose:** Kafka coordination
- **Volume:** zookeeper_data
- **Health Check:** nc -z localhost 2181

## Data Flow

### Application Startup

```
1. Zookeeper starts
   └─> Initializes coordination

2. PostgreSQL starts
   └─> Runs init scripts
       └─> Creates database

3. Redis starts
   └─> Loads AOF if exists

4. Kafka starts (waits for Zookeeper)
   └─> Registers with Zookeeper
       └─> Ready for topics

5. SQL SaaS Server starts (waits for all)
   └─> Connects to PostgreSQL
       └─> Runs Flyway migrations
           └─> Connects to Redis
               └─> Connects to Kafka
                   └─> Application Ready ✓
```

### Request Flow

```
Client
  │
  ├─> http://localhost:9090/api/...
  │
  └─> SQL SaaS Server
      │
      ├─> PostgreSQL (persistent data)
      │   └─> postgres:5432
      │
      ├─> Redis (caching)
      │   └─> redis:6379
      │
      └─> Kafka (events)
          └─> kafka:29092
```

## Network Configuration

### Internal Communication
- Service-to-service: Use service names (postgres, redis, kafka)
- DNS resolution: Automatic via Docker
- Network: sql-saas-network (bridge)

### External Access
- Application: localhost:9090
- PostgreSQL: localhost:5434
- Redis: localhost:6380
- Kafka: localhost:9092

## Volume Management

```
Persistent Data:
├─ postgres_data
│  └─ Database files, WAL logs
│
├─ redis_data
│  └─ AOF file, RDB snapshots
│
├─ kafka_data
│  └─ Topic partitions, logs
│
└─ zookeeper_data
   └─ Coordination data

Temporary Data:
└─ logs/ (bind mount)
   └─ Application logs
```

## Health Checks

```
Docker Compose monitors health of all services:

PostgreSQL:
  Command: pg_isready -U sqlsaas
  Interval: 10s
  
Redis:
  Command: redis-cli ping
  Interval: 10s
  
Kafka:
  Command: kafka-broker-api-versions
  Interval: 10s
  Start Period: 30s
  
SQL SaaS Server:
  Command: curl /actuator/health
  Interval: 30s
  Start Period: 60s
```

## Resource Allocation

### Development (Current)
```yaml
SQL SaaS Server:
  Memory: 1GB (JVM heap)
  CPU: Unlimited
  
PostgreSQL:
  Memory: Unlimited
  CPU: Unlimited
  
Redis:
  Memory: Unlimited
  CPU: Unlimited
  
Kafka:
  Memory: Unlimited
  CPU: Unlimited
```

### Production (Recommended)
```yaml
SQL SaaS Server:
  Memory: 2-4GB limit
  CPU: 2 cores
  
PostgreSQL:
  Memory: 2GB limit
  CPU: 2 cores
  Connection Pool: 20-50
  
Redis:
  Memory: 512MB-1GB limit
  MaxMemory Policy: allkeys-lru
  
Kafka:
  Memory: 2GB limit
  CPU: 2 cores
```

## Security Layers

```
Network:
├─ Isolated bridge network
├─ No direct host access
└─ Exposed ports only

Authentication:
├─ PostgreSQL: Username/Password
├─ Redis: Optional password support
└─ Application: Spring Security (if enabled)

Data:
├─ At Rest: Volume encryption (host level)
└─ In Transit: Optional SSL/TLS
```

## Build Process

```
Multi-Stage Docker Build:

Stage 1: Builder
├─ Base: gradle:8.5-jdk17-alpine
├─ Copy: Source code
├─ Build: ./gradlew bootJar
└─ Output: JAR file (~50-100MB)

Stage 2: Runtime
├─ Base: eclipse-temurin:17-jre-alpine
├─ Copy: JAR from builder
├─ Size: ~150-200MB total
└─ Run: java -jar sql-saas-platform.jar

Benefits:
├─ Smaller final image
├─ No build tools in production
├─ Layer caching
└─ Faster rebuilds
```

## Monitoring

```
Available Endpoints:

Health:
├─ /actuator/health
├─ /actuator/health/db
└─ /actuator/health/redis

Metrics:
├─ /actuator/metrics
└─ /actuator/prometheus

Info:
└─ /actuator/info

Logs:
├─ Docker: docker logs <container>
├─ File: ./logs/sql-saas-platform.log
└─ Stream: ./docker-manage.sh logs-server
```

## Failure Recovery

```
Automatic Recovery:

Container Crash:
├─ Restart Policy: unless-stopped
├─ Health Check: Detects failure
└─ Action: Automatic restart

Service Dependency:
├─ depends_on: Ensures order
├─ condition: Waits for health
└─ Retry: Application retries connection

Data Loss:
├─ Volumes: Persistent across restarts
├─ PostgreSQL: WAL for recovery
└─ Redis: AOF for persistence
```

## Scaling Considerations

```
Horizontal Scaling:
├─ SQL SaaS Server: ✓ Multiple instances
│   └─ Requires: Load balancer
├─ Kafka: ✓ Cluster mode
│   └─ Requires: Multiple brokers
└─ Redis: ✓ Cluster/Sentinel
    └─ Requires: Configuration changes

Vertical Scaling:
├─ Increase JVM heap
├─ Add more CPU cores
└─ Increase memory limits
```

---

This architecture provides:
- ✓ Service isolation
- ✓ Data persistence
- ✓ Health monitoring
- ✓ Automatic recovery
- ✓ Network security
- ✓ Easy development
- ✓ Production-ready foundation

## Container Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Host Machine                                 │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │           conductor-network (Bridge Network)               │   │
│  │                    172.28.0.0/16                           │   │
│  │                                                            │   │
│  │  ┌──────────────────────────────────────────────────┐    │   │
│  │  │          sql-saas-conductor-server               │    │   │
│  │  │                                                  │    │   │
│  │  │  - Spring Boot Application                       │    │   │
│  │  │  - Port: 8080                                    │    │   │
│  │  │  - JVM: -Xms1024M -Xmx2048M                     │    │   │
│  │  │  - Profile: docker                               │    │   │
│  │  │  - Health Check: /health                         │    │   │
│  │  │                                                  │    │   │
│  │  └──────────┬───────────┬──────────┬────────────────┘    │   │
│  │             │           │          │                      │   │
│  │             │           │          │                      │   │
│  │     ┌───────▼──┐  ┌────▼────┐  ┌──▼──────────┐         │   │
│  │     │PostgreSQL│  │  Redis  │  │    Kafka    │         │   │
│  │     │          │  │         │  │             │         │   │
│  │     │Port:5432 │  │Port:6379│  │Port:29092   │         │   │
│  │     │DB:       │  │DB:0     │  │Topics:auto  │         │   │
│  │     │conductor │  │AOF:yes  │  │             │         │   │
│  │     │          │  │         │  │             │         │   │
│  │     └──────────┘  └─────────┘  └──────┬──────┘         │   │
│  │                                        │                 │   │
│  │                                  ┌─────▼────────┐       │   │
│  │                                  │  Zookeeper   │       │   │
│  │                                  │              │       │   │
│  │                                  │  Port:2181   │       │   │
│  │                                  └──────────────┘       │   │
│  │                                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  Port Mappings (Host → Container):                              │
│  ├─ 5434 → postgres:5432                                       │
│  ├─ 6380 → redis:6379                                          │
│  ├─ 9092 → kafka:9092                                          │
│  └─ 8080 → conductor-server:8080                               │
│                                                                  │
│  Persistent Volumes:                                             │
│  ├─ postgres_data  → /var/lib/postgresql/data                  │
│  ├─ redis_data     → /data                                     │
│  ├─ kafka_data     → /var/lib/kafka/data                       │
│  └─ zookeeper_data → /var/lib/zookeeper/data                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Application Startup Sequence

```
1. PostgreSQL starts
   └─> Runs init-db scripts
       └─> Creates conductor database
           └─> Sets up extensions and permissions

2. Redis starts
   └─> Enables AOF persistence
       └─> Loads existing data (if any)

3. Zookeeper starts
   └─> Initializes coordination service

4. Kafka starts (depends on Zookeeper)
   └─> Connects to Zookeeper
       └─> Creates broker
           └─> Auto-creates topics as needed

5. Conductor Server starts (depends on all above)
   └─> Connects to PostgreSQL
       └─> Runs Flyway migrations
           └─> Connects to Redis
               └─> Connects to Kafka
                   └─> Application ready ✓
```

### 2. Health Check Flow

```
Docker Compose
    │
    ├─> PostgreSQL Health Check
    │   └─> pg_isready -U conductor
    │       └─> Returns: OK/FAIL
    │
    ├─> Redis Health Check
    │   └─> redis-cli ping
    │       └─> Returns: PONG/FAIL
    │
    ├─> Kafka Health Check
    │   └─> kafka-broker-api-versions
    │       └─> Returns: OK/FAIL
    │
    └─> Conductor Health Check
        └─> curl http://localhost:8080/health
            └─> Returns: {"status":"UP"}
```

### 3. Request Flow

```
Client Request
    │
    ├─> http://localhost:8080/api/...
    │
    └─> Conductor Server Container
        │
        ├─> [API Layer]
        │   └─> Validates request
        │       └─> Routes to service
        │
        ├─> [Service Layer]
        │   ├─> Reads/Writes PostgreSQL
        │   │   └─> postgres:5432
        │   │
        │   ├─> Caches in Redis
        │   │   └─> redis:6379
        │   │
        │   └─> Publishes events to Kafka
        │       └─> kafka:29092
        │
        └─> [Response]
            └─> JSON response to client
```

## Network Configuration

### Internal Network (conductor-network)

- **Type:** Bridge
- **Subnet:** 172.28.0.0/16
- **DNS:** Automatic service name resolution

Services can communicate using service names:
- `postgres` instead of IP address
- `redis` instead of IP address
- `kafka` instead of IP address

### External Access

| Service | Internal | External (Host) |
|---------|----------|-----------------|
| PostgreSQL | postgres:5432 | localhost:5434 |
| Redis | redis:6379 | localhost:6380 |
| Kafka | kafka:29092 | localhost:9092 |
| Conductor | conductor-server:8080 | localhost:8080 |

## Volume Mappings

### Persistent Data Volumes

```
postgres_data
    ├─ /var/lib/postgresql/data    (Database files)
    └─ Managed by: Docker

redis_data
    ├─ /data                        (AOF file, RDB snapshots)
    └─ Managed by: Docker

kafka_data
    ├─ /var/lib/kafka/data         (Topic partitions, logs)
    └─ Managed by: Docker

zookeeper_data
    ├─ /var/lib/zookeeper/data     (Zookeeper data)
    └─ Managed by: Docker

zookeeper_log
    ├─ /var/lib/zookeeper/log      (Transaction logs)
    └─ Managed by: Docker
```

### Bind Mounts

```
./logs → /app/logs                  (Application logs)
./docker/init-db → /docker-entrypoint-initdb.d  (DB init scripts)
```

## Resource Allocation

### Development Configuration

```yaml
PostgreSQL:
  Memory: Unlimited
  CPU: Unlimited
  Disk: ~1GB (typical)

Redis:
  Memory: Unlimited
  CPU: Unlimited
  Disk: ~100MB (typical)

Kafka:
  Memory: Unlimited
  CPU: Unlimited
  Disk: ~500MB (typical)

Zookeeper:
  Memory: Unlimited
  CPU: Unlimited
  Disk: ~100MB (typical)

Conductor Server:
  Memory: 2GB (JVM heap)
  CPU: Unlimited
  Disk: Minimal
```

### Production Configuration

```yaml
PostgreSQL:
  Memory: 2GB limit, 1GB reserved
  CPU: 2 cores
  Connection Pool: 200

Redis:
  Memory: 1GB limit, 512MB reserved
  MaxMemory Policy: allkeys-lru
  CPU: 1 core

Kafka:
  Memory: 2GB limit, 1GB reserved
  CPU: 2 cores
  Partitions: 3

Conductor Server:
  Memory: 6GB limit, 4GB reserved
  JVM Heap: -Xms2048M -Xmx4096M
  CPU: 2 cores limit, 1 core reserved
```

## Security Layers

```
┌─────────────────────────────────────────┐
│         Security Configuration          │
├─────────────────────────────────────────┤
│                                         │
│  Network Isolation                      │
│  ├─ Internal: conductor-network         │
│  ├─ External: Exposed ports only        │
│  └─ No direct container-to-host access  │
│                                         │
│  Authentication                          │
│  ├─ PostgreSQL: Username/Password       │
│  ├─ Redis: Optional password            │
│  └─ Conductor: JWT (if enabled)         │
│                                         │
│  Encryption                              │
│  ├─ In Transit: Optional SSL/TLS        │
│  └─ At Rest: Volume encryption          │
│                                         │
│  Access Control                          │
│  ├─ Container user: Non-root            │
│  ├─ Read-only mounts: Where possible    │
│  └─ Network policies: Restrict traffic  │
│                                         │
└─────────────────────────────────────────┘
```

## Build Process

```
┌─────────────────────────────────────────┐
│          Multi-Stage Build              │
├─────────────────────────────────────────┤
│                                         │
│  Stage 1: Builder                       │
│  ├─ Base: gradle:8.5-jdk17-alpine      │
│  ├─ Copy: Source code + dependencies    │
│  ├─ Build: ./gradlew bootJar            │
│  └─ Output: server-boot.jar (~200MB)    │
│                                         │
│  Stage 2: Runtime                       │
│  ├─ Base: eclipse-temurin:17-jre       │
│  ├─ Copy: JAR from builder stage        │
│  ├─ Size: ~300MB (vs ~800MB full)      │
│  └─ Run: java -jar conductor-server.jar │
│                                         │
└─────────────────────────────────────────┘
```

## Monitoring Points

```
Health Checks:
├─ PostgreSQL: pg_isready
├─ Redis: PING command
├─ Kafka: broker API versions
└─ Conductor: HTTP /health endpoint

Metrics:
├─ Application: /actuator/prometheus
├─ PostgreSQL: pg_stat_* views
├─ Redis: INFO command
└─ Kafka: JMX metrics

Logs:
├─ Container logs: docker logs <container>
├─ Application logs: ./logs/conductor.log
└─ Database logs: Volume mounted
```

## Failure Scenarios & Recovery

### Container Crashes

```
Scenario: Container exits unexpectedly
Recovery:
  1. Docker restart policy: unless-stopped
  2. Health check detects failure
  3. Container automatically restarts
  4. Depends_on ensures start order
```

### Data Corruption

```
Scenario: Volume data is corrupted
Recovery:
  1. Stop all services
  2. Remove corrupted volume
  3. Restore from backup
  4. Restart services
```

### Network Issues

```
Scenario: Network connectivity lost
Recovery:
  1. Services retry connections
  2. Circuit breakers prevent cascading failures
  3. Redis provides temporary cache
  4. Services recover when network restored
```

## Scaling Considerations

### Horizontal Scaling

```
Components that can scale:
├─ Conductor Server: Multiple instances
│   └─ Load balancer required
├─ Kafka: Multiple brokers
│   └─ Adjust replication factor
└─ Redis: Redis Cluster or Sentinel
    └─ Requires configuration changes

Components that don't scale horizontally:
├─ PostgreSQL (single instance)
│   └─ Use read replicas for reads
└─ Zookeeper (single instance)
    └─ Use ensemble for production
```

### Vertical Scaling

```
Adjust resources in docker-compose:
  deploy:
    resources:
      limits:
        memory: 4G
        cpus: '2'
```

---

This architecture provides:
- ✓ Service isolation
- ✓ Data persistence
- ✓ Health monitoring
- ✓ Automatic recovery
- ✓ Easy scaling
- ✓ Network security
