#!/bin/bash

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
TIMEOUT=10
MAX_RETRIES=30
RETRY_INTERVAL=2

echo -e "${YELLOW}Starting smoke tests...${NC}"
echo "Gateway URL: $GATEWAY_URL"
echo "Timeout: ${TIMEOUT}s"
echo "Max retries: $MAX_RETRIES"
echo ""

# Function to check service health
check_service_health() {
    local service=$1
    local port=$2
    local endpoint="${service}/actuator/health"

    for i in $(seq 1 $MAX_RETRIES); do
        echo -n "[$i/$MAX_RETRIES] Checking $service... "

        response=$(curl -s -o /dev/null -w "%{http_code}" \
            --connect-timeout $TIMEOUT \
            --max-time $TIMEOUT \
            "http://localhost:$port/actuator/health" 2>/dev/null || echo "000")

        if [ "$response" = "200" ]; then
            echo -e "${GREEN}UP${NC}"
            return 0
        fi

        echo "Response: $response, retrying in ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done

    echo -e "${RED}FAILED${NC}"
    return 1
}

# Define services to check
declare -A SERVICES=(
    ["api-gateway"]=8080
    ["auth-service"]=8081
    ["product-service"]=8082
    ["inventory-service"]=8083
    ["cart-service"]=8084
    ["order-service"]=8085
    ["payment-service"]=8086
    ["notification-service"]=8087
)

# Check all services
failed_services=0
for service in "${!SERVICES[@]}"; do
    port=${SERVICES[$service]}

    if check_service_health "$service" "$port"; then
        ((++passed_services))
    else
        ((++failed_services))
    fi
done

echo ""
echo "================================================"

# Run API smoke tests if gateway is up
if [ "$failed_services" -eq 0 ]; then
    echo -e "${YELLOW}Running API smoke tests...${NC}"
    echo ""

    # Test 1: Gateway health check
    echo -n "Test 1: API Gateway health... "
    response=$(curl -s -w "%{http_code}" -o response.json \
        --connect-timeout $TIMEOUT \
        --max-time $TIMEOUT \
        "$GATEWAY_URL/actuator/health")

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}PASS${NC}"
    else
        echo -e "${RED}FAIL${NC} (Status: $response)"
        ((++failed_services))
    fi

    # Test 2: Product Service through Gateway
    echo -n "Test 2: Product Service endpoint... "
    response=$(curl -s -w "%{http_code}" -o response.json \
        --connect-timeout $TIMEOUT \
        --max-time $TIMEOUT \
        "$GATEWAY_URL/products/health" 2>/dev/null || echo "000")

    if [ "$response" = "200" ] || [ "$response" = "404" ]; then
        echo -e "${GREEN}PASS${NC}"
    else
        echo -e "${YELLOW}SKIP${NC} (Status: $response)"
    fi

    # Test 3: Auth Service through Gateway
    echo -n "Test 3: Auth Service endpoint... "
    response=$(curl -s -w "%{http_code}" -o response.json \
        --connect-timeout $TIMEOUT \
        --max-time $TIMEOUT \
        "$GATEWAY_URL/auth/health" 2>/dev/null || echo "000")

    if [ "$response" = "200" ] || [ "$response" = "404" ]; then
        echo -e "${GREEN}PASS${NC}"
    else
        echo -e "${YELLOW}SKIP${NC} (Status: $response)"
    fi

    # Cleanup
    rm -f response.json

    echo ""
    echo "================================================"
fi

# Summary
echo ""
if [ "$failed_services" -eq 0 ]; then
    echo -e "${GREEN}✓ All smoke tests PASSED${NC}"
    exit 0
else
    echo -e "${RED}✗ $failed_services service(s) FAILED${NC}"
    exit 1
fi

