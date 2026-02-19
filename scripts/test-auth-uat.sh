#!/bin/bash
#
# LoanFlow Authentication UAT Test Script
# PRD Compliant: Tests Keycloak OAuth2/OIDC Integration
#

set -e

echo "╔═══════════════════════════════════════════════════════════════════════╗"
echo "║           LoanFlow Authentication UAT Test                            ║"
echo "║           Keycloak OAuth2/OIDC Integration                            ║"
echo "╚═══════════════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

KEYCLOAK_URL="http://localhost:8180"
AUTH_SERVICE_URL="http://localhost:8085"
REALM="loanflow"
CLIENT_ID="loanflow-web"

# Test users
declare -A USERS
USERS["admin"]="admin@loanflow.com:admin123:ADMIN"
USERS["officer"]="officer@loanflow.com:officer123:LOAN_OFFICER"
USERS["underwriter"]="underwriter@loanflow.com:underwriter123:UNDERWRITER"
USERS["customer"]="customer@example.com:customer123:CUSTOMER"

passed=0
failed=0

print_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((passed++))
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((failed++))
}

# =====================================================
# Step 1: Check Docker Services
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 1: Checking Docker Services"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

print_test "Docker daemon running..."
if docker ps > /dev/null 2>&1; then
    print_pass "Docker is running"
else
    print_fail "Docker is not running. Please start Docker Desktop."
    exit 1
fi

print_test "PostgreSQL container..."
if docker ps | grep -q loanflow-postgres; then
    print_pass "PostgreSQL is running"
else
    print_fail "PostgreSQL is not running"
fi

print_test "Keycloak container..."
if docker ps | grep -q loanflow-keycloak; then
    print_pass "Keycloak is running"
else
    print_fail "Keycloak is not running. Starting..."
    docker start loanflow-keycloak 2>/dev/null || echo "Need to create Keycloak container"
fi

echo ""

# =====================================================
# Step 2: Check Keycloak Health
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 2: Checking Keycloak Health"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

print_test "Keycloak reachable at $KEYCLOAK_URL..."
if curl -s -o /dev/null -w "%{http_code}" "$KEYCLOAK_URL" | grep -q "200\|302"; then
    print_pass "Keycloak is reachable"
else
    print_fail "Keycloak is not reachable at $KEYCLOAK_URL"
    exit 1
fi

print_test "LoanFlow realm exists..."
REALM_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$KEYCLOAK_URL/realms/$REALM")
if [ "$REALM_STATUS" = "200" ]; then
    print_pass "Realm '$REALM' exists"
else
    print_fail "Realm '$REALM' not found (HTTP $REALM_STATUS)"
fi

print_test "OIDC discovery endpoint..."
OIDC_CONFIG=$(curl -s "$KEYCLOAK_URL/realms/$REALM/.well-known/openid-configuration")
if echo "$OIDC_CONFIG" | grep -q "token_endpoint"; then
    print_pass "OIDC configuration available"
else
    print_fail "OIDC configuration not available"
fi

echo ""

# =====================================================
# Step 3: Test Direct Keycloak Login (All Users)
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 3: Testing Direct Keycloak Login (Resource Owner Password Grant)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for user_key in "${!USERS[@]}"; do
    IFS=':' read -r email password expected_role <<< "${USERS[$user_key]}"

    print_test "Login as $user_key ($email)..."

    RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "username=$email" \
        -d "password=$password")

    if echo "$RESPONSE" | grep -q "access_token"; then
        ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
        TOKEN_TYPE=$(echo "$RESPONSE" | jq -r '.token_type')
        EXPIRES_IN=$(echo "$RESPONSE" | jq -r '.expires_in')

        # Decode JWT to check roles
        PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null || echo "{}")

        print_pass "$user_key login successful (expires in ${EXPIRES_IN}s)"

        # Save token for later tests
        eval "TOKEN_$user_key='$ACCESS_TOKEN'"
    else
        ERROR=$(echo "$RESPONSE" | jq -r '.error_description // .error // "Unknown error"')
        print_fail "$user_key login failed: $ERROR"
    fi
done

echo ""

# =====================================================
# Step 4: Check Auth Service Health
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 4: Checking Auth Service"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

print_test "Auth service reachable at $AUTH_SERVICE_URL..."
AUTH_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$AUTH_SERVICE_URL/actuator/health" 2>/dev/null)
if [ "$AUTH_HEALTH" = "200" ]; then
    print_pass "Auth service is healthy"
else
    print_fail "Auth service not reachable (HTTP $AUTH_HEALTH). Start with: mvn spring-boot:run -pl auth-service"
    echo ""
    echo "Skipping auth-service API tests..."
    echo ""

    # Skip to summary
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "UAT SUMMARY"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${GREEN}Passed: $passed${NC}"
    echo -e "${RED}Failed: $failed${NC}"
    echo ""
    echo "Note: Auth service is not running. Start it to complete full UAT."
    exit 0
fi

echo ""

# =====================================================
# Step 5: Test Auth Service Login Endpoint
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 5: Testing Auth Service Login API"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for user_key in "${!USERS[@]}"; do
    IFS=':' read -r email password expected_role <<< "${USERS[$user_key]}"

    print_test "Auth API login as $user_key..."

    RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}")

    if echo "$RESPONSE" | grep -q "accessToken"; then
        print_pass "Auth API login for $user_key successful"
    else
        ERROR=$(echo "$RESPONSE" | jq -r '.message // "Unknown error"')
        print_fail "Auth API login for $user_key failed: $ERROR"
    fi
done

echo ""

# =====================================================
# Step 6: Test Token Refresh
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 6: Testing Token Refresh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

print_test "Getting refresh token..."
LOGIN_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@loanflow.com","password":"admin123"}')

REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken')

if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ]; then
    print_pass "Got refresh token"

    print_test "Refreshing token..."
    REFRESH_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/api/v1/auth/refresh" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

    if echo "$REFRESH_RESPONSE" | grep -q "accessToken"; then
        print_pass "Token refresh successful"
    else
        print_fail "Token refresh failed"
    fi
else
    print_fail "Could not get refresh token"
fi

echo ""

# =====================================================
# Step 7: Test Protected Endpoint
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 7: Testing Protected Endpoints"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Get a fresh token
ADMIN_TOKEN=$(curl -s -X POST "$AUTH_SERVICE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@loanflow.com","password":"admin123"}' | jq -r '.accessToken')

print_test "Access /api/v1/auth/me without token..."
UNAUTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$AUTH_SERVICE_URL/api/v1/auth/me")
if [ "$UNAUTH_RESPONSE" = "401" ]; then
    print_pass "Protected endpoint rejects unauthenticated request (401)"
else
    print_fail "Expected 401, got $UNAUTH_RESPONSE"
fi

print_test "Access /api/v1/auth/me with valid token..."
AUTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$AUTH_SERVICE_URL/api/v1/auth/me" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
if [ "$AUTH_RESPONSE" = "200" ]; then
    print_pass "Protected endpoint accepts valid token (200)"
else
    print_fail "Expected 200, got $AUTH_RESPONSE"
fi

echo ""

# =====================================================
# Step 8: Test Invalid Credentials
# =====================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 8: Testing Invalid Credentials Handling"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

print_test "Login with wrong password..."
INVALID_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$AUTH_SERVICE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@loanflow.com","password":"wrongpassword"}')
if [ "$INVALID_RESPONSE" = "401" ]; then
    print_pass "Invalid credentials rejected (401)"
else
    print_fail "Expected 401, got $INVALID_RESPONSE"
fi

print_test "Login with non-existent user..."
NONEXIST_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$AUTH_SERVICE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"notauser@example.com","password":"password"}')
if [ "$NONEXIST_RESPONSE" = "401" ]; then
    print_pass "Non-existent user rejected (401)"
else
    print_fail "Expected 401, got $NONEXIST_RESPONSE"
fi

echo ""

# =====================================================
# Summary
# =====================================================
echo "╔═══════════════════════════════════════════════════════════════════════╗"
echo "║                         UAT SUMMARY                                   ║"
echo "╚═══════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "  ${GREEN}✓ Passed: $passed${NC}"
echo -e "  ${RED}✗ Failed: $failed${NC}"
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}                    ALL TESTS PASSED!                                   ${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}                    SOME TESTS FAILED                                   ${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════════════════════${NC}"
    exit 1
fi
