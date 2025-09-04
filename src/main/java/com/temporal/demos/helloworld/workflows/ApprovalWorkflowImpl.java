package com.temporal.demos.helloworld.workflows;

import com.temporal.demos.helloworld.activities.ApprovalActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {
    
    private static final Logger logger = LoggerFactory.getLogger(ApprovalWorkflowImpl.class);
    
    private final ApprovalActivities activities = Workflow.newActivityStub(
            ApprovalActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(
                            io.temporal.common.RetryOptions.newBuilder()
                                    .setMaximumAttempts(3)
                                    .build()
                    )
                    .build()
    );
    
    // Workflow state
    private String approvalStatus = "PENDING";
    private String currentStep = "SUBMITTED";
    private String requestId;
    private String requestType;
    private String requestDetails;
    private String requesterEmail;
    private String approverEmail;
    private String approvalComments;
    private String rejectionReason;
    private Instant requestStartTime;
    private boolean approvalReceived = false;
    private boolean rejectionReceived = false;
    
    @Override
    public String processApprovalRequest(String requestId, String requestType, String requestDetails, String requesterEmail) {
        this.requestId = requestId;
        this.requestType = requestType;
        this.requestDetails = requestDetails;
        this.requesterEmail = requesterEmail;
        this.requestStartTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
        
        logger.info("Starting approval process for request: {}", requestId);
        
        // Step 1: Validate and prepare request
        currentStep = "VALIDATING";
        String validationResult = activities.validateRequest(requestId, requestType, requestDetails);
        
        if (!"VALID".equals(validationResult)) {
            currentStep = "REJECTED_VALIDATION";
            approvalStatus = "REJECTED";
            activities.notifyRequester(requesterEmail, "Request validation failed: " + validationResult);
            return "Request rejected during validation: " + validationResult;
        }
        
        // Step 2: Send notification to approvers
        currentStep = "AWAITING_APPROVAL";
        activities.notifyApprovers(requestId, requestType, requestDetails, requesterEmail);
        
        // Step 3: Wait for human approval/rejection (with timeout)
        logger.info("Waiting for approval decision for request: {}", requestId);
        
        // Wait for either approval or rejection signal, or timeout after 24 hours
        boolean decisionReceived = Workflow.await(
                Duration.ofHours(24),
                () -> approvalReceived || rejectionReceived
        );
        
        if (!decisionReceived) {
            // Timeout occurred
            currentStep = "TIMEOUT";
            approvalStatus = "TIMEOUT";
            activities.notifyRequester(requesterEmail, "Request timed out after 24 hours without approval");
            activities.notifyApprovers(requestId, requestType, "TIMEOUT - Request expired", requesterEmail);
            return "Request timed out after 24 hours";
        }
        
        // Process the decision
        if (approvalReceived) {
            currentStep = "APPROVED";
            approvalStatus = "APPROVED";
            
            // Execute approved action
            String executionResult = activities.executeApprovedAction(requestId, requestType, requestDetails);
            
            // Notify stakeholders
            activities.notifyRequester(requesterEmail, 
                    String.format("Request approved by %s. Comments: %s. Execution result: %s", 
                            approverEmail, approvalComments, executionResult));
            activities.logApprovalDecision(requestId, "APPROVED", approverEmail, approvalComments);
            
            currentStep = "COMPLETED";
            return String.format("Request approved and executed successfully. Approver: %s, Result: %s", 
                    approverEmail, executionResult);
            
        } else if (rejectionReceived) {
            currentStep = "REJECTED";
            approvalStatus = "REJECTED";
            
            // Notify stakeholders
            activities.notifyRequester(requesterEmail, 
                    String.format("Request rejected by %s. Reason: %s", approverEmail, rejectionReason));
            activities.logApprovalDecision(requestId, "REJECTED", approverEmail, rejectionReason);
            
            currentStep = "COMPLETED";
            return String.format("Request rejected by %s. Reason: %s", approverEmail, rejectionReason);
        }
        
        return "Unexpected workflow state";
    }
    
    @Override
    public void approve(String approverEmail, String comments) {
        logger.info("Approval received from: {} for request: {}", approverEmail, requestId);
        this.approverEmail = approverEmail;
        this.approvalComments = comments != null ? comments : "No comments provided";
        this.approvalReceived = true;
    }
    
    @Override
    public void reject(String approverEmail, String reason) {
        logger.info("Rejection received from: {} for request: {}", approverEmail, requestId);
        this.approverEmail = approverEmail;
        this.rejectionReason = reason != null ? reason : "No reason provided";
        this.rejectionReceived = true;
    }
    
    @Override
    public String getApprovalStatus() {
        return approvalStatus;
    }
    
    @Override
    public String getRequestDetails() {
        return String.format("Request ID: %s, Type: %s, Details: %s, Requester: %s", 
                requestId, requestType, requestDetails, requesterEmail);
    }
    
    @Override
    public String getCurrentStep() {
        return currentStep;
    }
    
    @Override
    public long getWaitingTimeInSeconds() {
        if (requestStartTime == null) {
            return 0;
        }
        return Duration.between(requestStartTime, Instant.ofEpochMilli(Workflow.currentTimeMillis())).getSeconds();
    }
}