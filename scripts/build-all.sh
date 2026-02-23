#!/usr/bin/env bash
#
# build-all.sh — Build all LoanFlow backend services and frontend
#
# This script:
#   1. Builds common modules and all Spring Boot JARs (skip tests by default)
#   2. Optionally builds Docker images for all services
#
# Usage:
#   ./scripts/build-all.sh              # Build JARs only
#   ./scripts/build-all.sh --docker     # Build JARs + Docker images
#   ./scripts/build-all.sh --test       # Build JARs with tests enabled
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse flags
RUN_TESTS=false
BUILD_DOCKER=false
for arg in "$@"; do
    case $arg in
        --test)   RUN_TESTS=true ;;
        --docker) BUILD_DOCKER=true ;;
        --help)
            echo "Usage: $0 [--test] [--docker]"
            echo "  --test    Run tests during Maven build"
            echo "  --docker  Build Docker images after Maven build"
            exit 0
            ;;
    esac
done

# Java version guard
export JAVA_HOME="${JAVA_HOME:-/Users/rayan/Library/Java/JavaVirtualMachines/openjdk-20.0.1/Contents/Home}"
JAVA_VER=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)
echo -e "${GREEN}Using Java: $JAVA_VER${NC}"
echo -e "${GREEN}JAVA_HOME: $JAVA_HOME${NC}"

# ──────────────────────────────────────────────────────────────────
# Step 1: Build all backend modules with Maven
# ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${YELLOW}=== Building Backend (Maven) ===${NC}"

SKIP_FLAG=""
if [ "$RUN_TESTS" = false ]; then
    SKIP_FLAG="-DskipTests"
    echo -e "  Tests: ${YELLOW}SKIPPED${NC} (use --test to enable)"
fi

cd "$PROJECT_ROOT/backend"
mvn clean install $SKIP_FLAG -B

echo -e "${GREEN}Backend build complete.${NC}"

# ──────────────────────────────────────────────────────────────────
# Step 2 (optional): Build Docker images
# ──────────────────────────────────────────────────────────────────
if [ "$BUILD_DOCKER" = true ]; then
    echo ""
    echo -e "${YELLOW}=== Building Docker Images ===${NC}"

    SERVICES=(
        "api-gateway"
        "auth-service"
        "customer-service"
        "loan-service"
        "document-service"
        "policy-service"
        "notification-service"
    )

    for svc in "${SERVICES[@]}"; do
        echo -e "  Building ${GREEN}loanflow/$svc${NC}..."
        docker build \
            -t "loanflow/$svc:latest" \
            -f "$PROJECT_ROOT/infrastructure/dockerfiles/backend/Dockerfile" \
            "$PROJECT_ROOT/backend/$svc"
    done

    # Frontend
    echo -e "  Building ${GREEN}loanflow/frontend${NC}..."
    docker build \
        -t "loanflow/frontend:latest" \
        -f "$PROJECT_ROOT/infrastructure/dockerfiles/frontend/Dockerfile" \
        "$PROJECT_ROOT/frontend/loanflow-web"

    echo -e "${GREEN}Docker images built successfully.${NC}"
    echo ""
    docker images | grep loanflow
fi

echo ""
echo -e "${GREEN}=== Build Complete ===${NC}"
echo ""
echo "Next steps:"
echo "  1. Start infrastructure:  docker compose -f infrastructure/docker-compose.yml up -d"
echo "  2. Start services:        docker compose -f infrastructure/docker-compose.services.yml up -d"
echo "  Or run everything:        docker compose -f infrastructure/docker-compose.yml -f infrastructure/docker-compose.services.yml up -d"
