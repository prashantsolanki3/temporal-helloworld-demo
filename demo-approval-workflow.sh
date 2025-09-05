#!/bin/bash

# Temporal Human-in-the-Loop Approval Demo Script
# This script demonstrates the complete approval workflow with interactive user input

echo "🚀 Temporal Human-in-the-Loop Approval Demo"
echo "============================================="
echo "This demo showcases:"
echo "• 📡 Signals for external events (approve/reject)"  
echo "• ❓ Queries for real-time workflow state"
echo "• ⏱️  Long-running workflows with human decisions"
echo "• 🔄 Durable execution with state persistence"
echo ""

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8090/api/approval"

# Check if application is running
echo -e "${BLUE}🔍 Checking if application is running...${NC}"
if ! curl -s http://localhost:8090/actuator/health >/dev/null 2>&1; then
    if ! curl -s http://localhost:8090/api/hello >/dev/null 2>&1; then
        echo -e "${RED}❌ Application is not running on localhost:8090${NC}"
        echo -e "${YELLOW}Please start the application first:${NC}"
        echo "mvn spring-boot:run"
        exit 1
    fi
fi
echo -e "${GREEN}✅ Application is running${NC}"
echo ""

echo -e "${BLUE}Step 1: Submitting an access request...${NC}"
RESPONSE=$(curl -s -X POST $BASE_URL/request \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "REQ-ACCESS-001",
    "requestDetails": "Need access to production database for urgent customer data analysis",
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

echo -e "${GREEN}✅ Access request submitted successfully!${NC}"
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

# Interactive decision making
echo -e "${BLUE}Step 5: Human Decision Required${NC}"
echo -e "${YELLOW}What would you like to do?${NC}"
echo "1) Approve the access request"
echo "2) Reject the access request"
echo "3) Check status and wait"
echo ""
read -p "Enter your choice (1-3): " CHOICE

case $CHOICE in
    1)
        echo ""
        echo -e "${GREEN}👍 Processing approval...${NC}"
        
        # Get approver email
        read -p "Enter approver email (or press Enter for default): " APPROVER_EMAIL
        if [ -z "$APPROVER_EMAIL" ]; then
            APPROVER_EMAIL="security-manager@company.com"
        fi
        
        # Get approval comments
        read -p "Enter approval comments (or press Enter for default): " APPROVAL_COMMENTS
        if [ -z "$APPROVAL_COMMENTS" ]; then
            APPROVAL_COMMENTS="Approved for emergency access. Please revoke after analysis is complete."
        fi
        
        APPROVAL_RESPONSE=$(curl -s -X POST $BASE_URL/approve/$WORKFLOW_ID \
          -H "Content-Type: application/json" \
          -d "{
            \"approverEmail\": \"$APPROVER_EMAIL\",
            \"comments\": \"$APPROVAL_COMMENTS\"
          }")
        
        echo "Approval Response: $APPROVAL_RESPONSE"
        echo ""
        echo -e "${GREEN}✅ Approval signal sent!${NC}"
        ;;
        
    2)
        echo ""
        echo -e "${RED}👎 Processing rejection...${NC}"
        
        # Get approver email
        read -p "Enter approver email (or press Enter for default): " APPROVER_EMAIL
        if [ -z "$APPROVER_EMAIL" ]; then
            APPROVER_EMAIL="security-manager@company.com"
        fi
        
        # Get rejection reason
        read -p "Enter rejection reason (or press Enter for default): " REJECTION_REASON
        if [ -z "$REJECTION_REASON" ]; then
            REJECTION_REASON="Access request denied. Please submit request through proper security channels."
        fi
        
        REJECTION_RESPONSE=$(curl -s -X POST $BASE_URL/reject/$WORKFLOW_ID \
          -H "Content-Type: application/json" \
          -d "{
            \"approverEmail\": \"$APPROVER_EMAIL\",
            \"reason\": \"$REJECTION_REASON\"
          }")
        
        echo "Rejection Response: $REJECTION_RESPONSE"
        echo ""
        echo -e "${RED}❌ Rejection signal sent!${NC}"
        ;;
        
    3)
        echo ""
        echo -e "${BLUE}📊 Checking current status...${NC}"
        STATUS_RESPONSE=$(curl -s $BASE_URL/status/$WORKFLOW_ID)
        echo "Current Status: $STATUS_RESPONSE"
        echo ""
        echo -e "${YELLOW}⏳ Workflow is still waiting for approval. You can run this script again or use the API directly.${NC}"
        echo -e "${BLUE}Manual approval commands:${NC}"
        echo "Approve: curl -X POST $BASE_URL/approve/$WORKFLOW_ID -H \"Content-Type: application/json\" -d '{\"approverEmail\": \"security-manager@company.com\", \"comments\": \"Your comments\"}'"
        echo "Reject:  curl -X POST $BASE_URL/reject/$WORKFLOW_ID -H \"Content-Type: application/json\" -d '{\"approverEmail\": \"security-manager@company.com\", \"reason\": \"Your reason\"}'"
        exit 0
        ;;
        
    *)
        echo -e "${RED}❌ Invalid choice. Exiting...${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}Step 6: Checking final status after decision...${NC}"
sleep 3
FINAL_STATUS=$(curl -s $BASE_URL/status/$WORKFLOW_ID)
echo "Final Status: $FINAL_STATUS"
echo ""

echo -e "${BLUE}Step 7: Getting final workflow result...${NC}"
sleep 2
FINAL_RESULT=$(curl -s $BASE_URL/result/$WORKFLOW_ID)
echo "Final Result: $FINAL_RESULT"
echo ""

echo -e "${GREEN}🎉 Interactive demo completed successfully!${NC}"
echo ""
echo -e "${YELLOW}📋 Summary of what happened:${NC}"
echo "1. ✅ Submitted access request to Temporal workflow"
echo "2. ✅ Workflow validated request and notified approvers"
echo "3. ✅ Workflow waited for human decision (you!)"
echo "4. ✅ Human signal was sent to workflow"
echo "5. ✅ Workflow processed decision and granted/denied access"
echo "6. ✅ Final result was returned"
echo ""
echo -e "${YELLOW}Key Temporal Concepts Demonstrated:${NC}"
echo "• ⏱️  Long-running workflows that wait for human input"
echo "• 📡 Signals for external events (approve/reject)"
echo "• ❓ Queries for real-time workflow state inspection"
echo "• 🔄 Durable execution with state persistence"
echo "• ⏰ Configurable timeouts and error handling"
echo "• 🎯 Complete workflow orchestration with activities"
echo ""

echo -e "${BLUE}🔄 Want to test more scenarios?${NC}"
echo "• Run this script again for another access request workflow"
echo "• Test the timeout feature by not making a decision (24-hour timeout)"
echo "• Try different request details and see how the workflow handles them"
echo ""

echo -e "${BLUE}🔧 Direct API Usage:${NC}"
echo "• Check any workflow status: curl $BASE_URL/status/WORKFLOW_ID"
echo "• Get workflow result: curl $BASE_URL/result/WORKFLOW_ID"
echo "• Temporal UI (if running): http://localhost:8080"