#!/bin/bash

set -euo pipefail

# Ensure we're running bash, not zsh
if [ -z "${BASH_VERSION:-}" ]; then
    exec bash "$0" "$@"
fi

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRASTRUCTURE_DIR="${PROJECT_ROOT}/infrastructure/docker"
SERVICES_DIR="${PROJECT_ROOT}/services"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts"

# Service configuration - using parallel arrays
SERVICES_NAMES=("api-gateway" "auth-service" "product-service" "inventory-service" "cart-service" "order-service" "payment-service" "notification-service")
SERVICES_PORTS=(8080 8081 8082 8083 8084 8085 8086 8087)

TIMEOUT=300  # 5 minutes overall timeout
RETRY_INTERVAL=2
HEALTH_MAX_RETRIES=60  # 2 minutes per service
SERVICE_PIDS=()

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Cleanup function
cleanup() {
    log_warning "Cleaning up..."
    # Kill all background services if they're running
    kill_services
}

trap cleanup EXIT

# Kill all background service processes
kill_services() {
    if [ ${#SERVICE_PIDS[@]} -eq 0 ]; then
        return
    fi
    for pid in "${SERVICE_PIDS[@]}"; do
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            log_info "Stopping service with PID $pid..."
            kill "$pid" || true
        fi
    done
}

# Check if Docker is running
check_docker() {
    log_info "Checking Docker daemon..."
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    log_success "Docker is running"
}

# Start infrastructure services
start_infrastructure() {
    log_info "Starting infrastructure services with Docker Compose..."
    cd "${INFRASTRUCTURE_DIR}"

    # Stop any existing services
    docker-compose down -v 2>/dev/null || true

    # Start services in the background
    docker-compose up -d

    # Wait for services to be healthy
    log_info "Waiting for infrastructure services to be healthy..."

    local services=("postgres" "redis" "zookeeper" "kafka" "cassandra" "mongodb" "elasticsearch")

    for service in "${services[@]}"; do
        log_info "Waiting for $service..."
        local max_attempts=30
        local attempt=0

        while [ $attempt -lt $max_attempts ]; do
            if docker-compose logs "$service" 2>&1 | grep -q "healthy\|started\|ready"; then
                log_success "$service is healthy"
                break
            fi

            # Additional checks for specific services
            case "$service" in
                postgres)
                    if docker exec ecommerce-postgres pg_isready -U ecommerce -d ecommerce > /dev/null 2>&1; then
                        log_success "$service is healthy"
                        break
                    fi
                    ;;
                redis)
                    if docker exec ecommerce-redis redis-cli ping > /dev/null 2>&1; then
                        log_success "$service is healthy"
                        break
                    fi
                    ;;
                kafka)
                    if docker exec ecommerce-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
                        log_success "$service is healthy"
                        break
                    fi
                    ;;
                mongodb)
                    if docker exec ecommerce-mongodb mongosh --quiet --eval 'db.runCommand({ ping: 1 })' mongodb://ecommerce:ecommerce@localhost:27017/admin > /dev/null 2>&1; then
                        log_success "$service is healthy"
                        break
                    fi
                    ;;
            esac

            ((attempt++))
            if [ $attempt -lt $max_attempts ]; then
                sleep $RETRY_INTERVAL
            fi
        done

        if [ $attempt -eq $max_attempts ]; then
            log_warning "$service may not be fully ready yet, but continuing..."
        fi
    done

    log_success "Infrastructure services started"
    cd "${PROJECT_ROOT}"
}

# Build the project
build_project() {
    log_info "Building the entire project with Maven..."
    cd "${PROJECT_ROOT}"

    if command -v mvn &> /dev/null; then
        mvn clean install -DskipTests -q
        log_success "Project built successfully"
    else
        log_error "Maven is not installed. Please install Maven and try again."
        exit 1
    fi
}

# Start a single service
start_service() {
    local service=$1
    local port=$2
    local jar_path="${SERVICES_DIR}/${service}/target/${service}.jar"

    if [ ! -f "$jar_path" ]; then
        log_error "JAR file not found: $jar_path"
        return 1
    fi

    log_info "Starting $service on port $port..."

    # Start service in background
    java -Dserver.port=$port -jar "$jar_path" > "/tmp/${service}.log" 2>&1 &
    local pid=$!

    SERVICE_PIDS+=("$pid")
    log_success "Started $service (PID: $pid)"
}

# Check if a service is healthy
check_service_health() {
    local service=$1
    local port=$2

    local response=$(curl -s -o /dev/null -w "%{http_code}" \
        --connect-timeout 5 \
        --max-time 5 \
        "http://localhost:$port/actuator/health" 2>/dev/null || echo "000")

    [ "$response" = "200" ]
}

# Wait for a service to be healthy
wait_for_service() {
    local service=$1
    local port=$2
    local max_retries=$HEALTH_MAX_RETRIES
    local attempt=0

    log_info "Waiting for $service to be healthy (port $port)..."

    while [ $attempt -lt $max_retries ]; do
        if check_service_health "$service" "$port"; then
            log_success "$service is healthy"
            return 0
        fi

        ((attempt++))
        if [ $attempt -lt $max_retries ]; then
            echo -n "."
            sleep $RETRY_INTERVAL
        fi
    done

    echo ""
    log_error "$service failed to become healthy after ${max_retries} attempts"
    log_warning "Checking service logs at /tmp/${service}.log"
    tail -n 50 "/tmp/${service}.log" || true
    return 1
}

# Start all services
start_all_services() {
    log_info "Starting all microservices..."

    SERVICE_PIDS=()

    # Start all services in parallel
    for i in "${!SERVICES_NAMES[@]}"; do
        local service="${SERVICES_NAMES[$i]}"
        local port="${SERVICES_PORTS[$i]}"
        start_service "$service" "$port" &
    done

    # Wait for all start commands to complete
    wait

    # Now wait for all services to be healthy
    log_info "Waiting for all services to be healthy..."

    local failed_services=0
    for i in "${!SERVICES_NAMES[@]}"; do
        local service="${SERVICES_NAMES[$i]}"
        local port="${SERVICES_PORTS[$i]}"

        if ! wait_for_service "$service" "$port"; then
            ((failed_services++))
        fi
    done

    if [ $failed_services -gt 0 ]; then
        log_error "$failed_services service(s) failed to start"
        return 1
    fi

    log_success "All services are healthy"
}

# Run the curl sequence tests
run_curl_tests() {
    log_info "Running API smoke tests..."

    if [ ! -f "${SCRIPTS_DIR}/smoke-tests.sh" ]; then
        log_error "smoke-tests.sh not found"
        return 1
    fi

    bash "${SCRIPTS_DIR}/smoke-tests.sh"
    log_success "Smoke tests completed"

    log_info "Running comprehensive curl sequence tests..."

    if [ ! -f "${SCRIPTS_DIR}/run-gateway-sequence.sh" ]; then
        log_error "run-gateway-sequence.sh not found"
        return 1
    fi

    bash "${SCRIPTS_DIR}/run-gateway-sequence.sh"
    log_success "Curl sequence tests completed"
}

# Main execution
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   E-Commerce Platform - Start All Services & Test      ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""

    log_info "Starting execution at $(date)"
    log_info "Project root: $PROJECT_ROOT"
    echo ""

    # Step 1: Check Docker
    log_info "=== Step 1/5: Checking Docker ==="
    check_docker
    echo ""

    # Step 2: Start infrastructure
    log_info "=== Step 2/5: Starting Infrastructure ==="
    start_infrastructure
    echo ""

    # Step 3: Build project
    log_info "=== Step 3/5: Building Project ==="
    build_project
    echo ""

    # Step 4: Start all services
    log_info "=== Step 4/5: Starting Microservices ==="
    start_all_services
    echo ""

    # Step 5: Run tests
    log_info "=== Step 5/5: Running Tests ==="
    if run_curl_tests; then
        echo ""
        echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║   ✓ ALL TESTS PASSED SUCCESSFULLY!                    ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
        echo ""
        log_success "Completed execution at $(date)"
        return 0
    else
        echo ""
        echo -e "${RED}╔════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║   ✗ SOME TESTS FAILED                                 ║${NC}"
        echo -e "${RED}╚════════════════════════════════════════════════════════╝${NC}"
        echo ""
        log_error "Completed execution at $(date)"
        return 1
    fi
}

# Run main
main "$@"

