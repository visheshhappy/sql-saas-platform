#!/bin/bash

# Docker Management Script for SQL SaaS Platform
# This script provides convenient commands to manage the Docker environment

set -e

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
PROJECT_NAME="sql-saas-platform"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    cat << EOF
Docker Management Script for SQL SaaS Platform

Usage: ./docker-manage.sh [COMMAND]

Commands:
  start           Start all services in detached mode
  stop            Stop all services
  restart         Restart all services
  build           Build the sql-saas-server image
  rebuild         Rebuild and restart all services
  logs            Show logs for all services
  logs-server     Show logs for sql-saas-server only
  status          Show status of all services
  clean           Stop and remove all containers and volumes
  shell-postgres  Open PostgreSQL shell
  shell-redis     Open Redis CLI
  shell-server    Open bash shell in sql-saas-server
  test-kafka      Test Kafka connectivity
  health          Check health of all services
  help            Show this help message

Examples:
  ./docker-manage.sh start
  ./docker-manage.sh logs-server
  ./docker-manage.sh rebuild

EOF
}

start_services() {
    print_info "Starting all services..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build
    print_success "All services started"
    print_info "Access SQL SaaS Platform at: http://localhost:9090"
    print_info "PostgreSQL available at: localhost:5434 (user: sqlsaas, password: sqlsaas123)"
    print_info "Redis available at: localhost:6380"
    print_info "Kafka available at: localhost:9092"
}

stop_services() {
    print_info "Stopping all services..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down
    print_success "All services stopped"
}

restart_services() {
    print_info "Restarting all services..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" restart
    print_success "All services restarted"
}

build_images() {
    print_info "Building sql-saas-server image..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build sql-saas-server
    print_success "Build completed"
}

rebuild_services() {
    print_info "Rebuilding and restarting all services..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build
    print_success "Rebuild completed"
}

show_logs() {
    print_info "Showing logs for all services (Ctrl+C to exit)..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f
}

show_server_logs() {
    print_info "Showing logs for sql-saas-server (Ctrl+C to exit)..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f sql-saas-server
}

show_status() {
    print_info "Service Status:"
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
}

clean_environment() {
    print_warning "This will stop and remove all containers, networks, and volumes!"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        print_info "Cleaning up environment..."
        docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v --remove-orphans
        print_success "Environment cleaned"
    else
        print_info "Cleanup cancelled"
    fi
}

postgres_shell() {
    print_info "Opening PostgreSQL shell..."
    docker exec -it sql-saas-postgres psql -U sqlsaas -d sqlsaasdb
}

redis_shell() {
    print_info "Opening Redis CLI..."
    docker exec -it sql-saas-redis redis-cli -p 6379
}

server_shell() {
    print_info "Opening bash shell in sql-saas-server..."
    docker exec -it sql-saas-server sh
}

test_kafka() {
    print_info "Testing Kafka connectivity..."
    
    # List topics
    print_info "Listing Kafka topics:"
    docker exec sql-saas-kafka kafka-topics --list --bootstrap-server localhost:9092
    
    # Create test topic
    print_info "Creating test topic 'test-topic'..."
    docker exec sql-saas-kafka kafka-topics --create --if-not-exists --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
    
    print_success "Kafka is working correctly"
}

check_health() {
    print_info "Checking service health..."
    
    # Check PostgreSQL
    if docker exec sql-saas-postgres pg_isready -U sqlsaas > /dev/null 2>&1; then
        print_success "PostgreSQL is healthy"
    else
        print_error "PostgreSQL is not healthy"
    fi
    
    # Check Redis
    if docker exec sql-saas-redis redis-cli ping > /dev/null 2>&1; then
        print_success "Redis is healthy"
    else
        print_error "Redis is not healthy"
    fi
    
    # Check Kafka
    if docker exec sql-saas-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        print_success "Kafka is healthy"
    else
        print_error "Kafka is not healthy"
    fi
    
    # Check SQL SaaS Server
    if curl -f http://localhost:9090/actuator/health > /dev/null 2>&1; then
        print_success "SQL SaaS Server is healthy"
    else
        print_error "SQL SaaS Server is not healthy"
    fi
}

# Main script logic
case "$1" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    build)
        build_images
        ;;
    rebuild)
        rebuild_services
        ;;
    logs)
        show_logs
        ;;
    logs-server)
        show_server_logs
        ;;
    status)
        show_status
        ;;
    clean)
        clean_environment
        ;;
    shell-postgres)
        postgres_shell
        ;;
    shell-redis)
        redis_shell
        ;;
    shell-server)
        server_shell
        ;;
    test-kafka)
        test_kafka
        ;;
    health)
        check_health
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
