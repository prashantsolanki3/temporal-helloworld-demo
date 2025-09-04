#!/bin/bash

# Temporal Human-in-the-Loop Approval Demo Script
# This script demonstrates the complete approval workflow

echo "🚀 Temporal Human-in-the-Loop Approval Demo"
echo "============================================="
echo ""

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8090/api/approval"

echo -e "${BLUE}Step 1: Submitting an approval request...${NC}"
RESPONSE=$(curl -s -X POST $BASE_URL/request \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "REQ-DEMO-001",
    "requestType": "BUDGET_REQUEST",
    "requestDetails": "Need $75,000 for new development server infrastructure and CI/CD tools",
    "requesterEmail": "developer@company.com"
  }')

echo "Response: $RESPONSE"
echo ""

# Extract workflow ID from response
WORKFLOW_ID=$(echo $RESPONSE | grep -o '"workflowId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$WORKFLOW_ID" ]; then
    echo -e "${RED}❌ Failed to get workflow ID from response${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Approval request submitted successfully!${NC}"
echo -e "${YELLOW}Workflow ID: $WORKFLOW_ID${NC}"
echo ""

echo -e "${BLUE}Step 2: Checking initial status...${NC}"
sleep 3
STATUS_RESPONSE=$(curl -s $BASE_URL/status/$WORKFLOW_ID)
echo "Status: $STATUS_RESPONSE"
echo ""

echo -e "${BLUE}Step 3: Waiting for workflow to reach approval stage...${NC}"
sleep 5

echo -e "${BLUE}Step 4: Checking status while waiting for approval...${NC}"
STATUS_RESPONSE=$(curl -s $BASE_URL/status/$WORKFLOW_ID)
echo "Status: $STATUS_RESPONSE"
echo ""

echo -e "${YELLOW}⏳ Workflow is now waiting for human approval...${NC}"
echo ""

echo -e "${BLUE}Step 5: Simulating manager approval...${NC}"
APPROVAL_RESPONSE=$(curl -s -X POST $BASE_URL/approve/$WORKFLOW_ID \
  -H "Content-Type: application/json" \
  -d '{
    "approverEmail": "manager@company.com",
    "comments": "Approved for Q4 infrastructure budget. Critical for team productivity."
  }')

echo "Approval Response: $APPROVAL_RESPONSE"
echo ""

echo -e "${GREEN}✅ Approval signal sent!${NC}"
echo ""

echo -e "${BLUE}Step 6: Checking final status after approval...${NC}"
sleep 3
FINAL_STATUS=$(curl -s $BASE_URL/status/$WORKFLOW_ID)
echo "Final Status: $FINAL_STATUS"
echo ""

echo -e "${BLUE}Step 7: Getting final workflow result...${NC}"
sleep 2
FINAL_RESULT=$(curl -s $BASE_URL/result/$WORKFLOW_ID)
echo "Final Result: $FINAL_RESULT"
echo ""

echo -e "${GREEN}🎉 Demo completed successfully!${NC}"
echo ""
echo -e "${YELLOW}Key Temporal Concepts Demonstrated:${NC}"
echo "• ⏱️  Long-running workflows that wait for human input"
echo "• 📡 Signals for external events (approve/reject)"
echo "• ❓ Queries for real-time workflow state inspection"
echo "• 🔄 Durable execution with state persistence"
echo "• ⏰ Configurable timeouts and error handling"
echo "• 🎯 Complete workflow orchestration with activities"
echo ""

echo -e "${BLUE}Alternative Test: Rejection Demo${NC}"
echo "To test rejection, submit another request and use:"
echo "curl -X POST $BASE_URL/reject/WORKFLOW_ID -H \"Content-Type: application/json\" -d '{\"approverEmail\": \"manager@company.com\", \"reason\": \"Budget not available for this quarter\"}'"
echo ""

echo -e "${BLUE}Monitoring Options:${NC}"
echo "• Check status: curl $BASE_URL/status/WORKFLOW_ID"
echo "• Get result: curl $BASE_URL/result/WORKFLOW_ID"
echo "• Temporal UI: http://localhost:8080 (if Temporal server is running)"