#!/bin/bash

# Demo script to test Temporal retry functionality with random errors
# This script demonstrates how to use the orchestration endpoints to test retry behavior

BASE_URL="http://localhost:8090/api/orchestration"
USER_ID="test-user-$(date +%s)"

echo "=== Temporal Retry Functionality Demo ==="
echo "User ID: $USER_ID"
echo "Base URL: $BASE_URL"
echo ""

# Function to make HTTP requests with error handling
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    echo "Making $method request to: $endpoint"
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        curl -s -X POST -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint" | jq .
    elif [ "$method" = "POST" ]; then
        curl -s -X POST -H "Content-Type: application/json" "$BASE_URL$endpoint" | jq .
    else
        curl -s "$BASE_URL$endpoint" | jq .
    fi
    echo ""
}

# Step 1: Check current error simulation status
echo "1. Checking current error simulation status..."
make_request "GET" "/error-simulation/status"

# Step 2: Enable error simulation
echo "2. Enabling error simulation..."
make_request "POST" "/error-simulation/enable"

# Step 3: Run a test orchestration to see retries in action
echo "3. Running orchestration with error simulation enabled (this may take a while due to retries)..."
echo "   Watch the application logs to see retry attempts in action!"
make_request "POST" "/execute-sync" "{\"userId\":\"$USER_ID\"}"

# Step 3b: Test async payment pattern specifically
echo "3b. Testing ASYNC PAYMENT pattern with polling (may take several minutes)..."
echo "    This demonstrates server-side retries pattern for polling async services!"
make_request "POST" "/execute-sync" "{\"userId\":\"$USER_ID-async\",\"useAsyncPayment\":true}"

# Step 4: Check error simulation status again
echo "4. Checking error simulation status after test..."
make_request "GET" "/error-simulation/status"

# Step 5: Optionally disable error simulation
echo "5. Do you want to disable error simulation? (y/n)"
read -r disable_errors
if [[ $disable_errors =~ ^[Yy]$ ]]; then
    echo "Disabling error simulation..."
    make_request "POST" "/error-simulation/disable"
    
    echo "6. Running orchestration without errors to compare..."
    make_request "POST" "/execute-sync" "{\"userId\":\"$USER_ID-no-errors\"}"
fi

echo ""
echo "=== Demo Complete ==="
echo "Tips for testing:"
echo "1. Check application logs to see retry attempts"
echo "2. Use /execute (async) for long-running tests"
echo "3. Use /status/{workflowId} to check progress"
echo "4. Try multiple executions to see different error patterns"
echo "5. Error rates are configurable in RandomErrorGenerator class"