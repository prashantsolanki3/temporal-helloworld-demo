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
    
    // Simplified workflow state
    private String approvalStatus = "PENDING";
    private String currentStep = "SUBMITTED";
    private String requestId;
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
        this.requestDetails = requestDetails;
        this.requesterEmail = requesterEmail;
        this.requestStartTime = Instant.ofEpochMilli(Workflow.currentTimeMillis());
        
        logger.info("Starting approval process for access request: {}", requestId);
        
        // Step 1: Validate request
        currentStep = "VALIDATING";
        String validationResult = activities.validateRequest(requestId, requestDetails);
        
        if (!"VALID".equals(validationResult)) {
            currentStep = "REJECTED_VALIDATION";
            approvalStatus = "REJECTED";
            activities.notifyRequester(requesterEmail, "Request validation failed: " + validationResult);
            return "Request rejected during validation: " + validationResult;
        }
        
        // Step 2: Notify approvers
        currentStep = "AWAITING_APPROVAL";
        activities.notifyApprovers(requestId, requestDetails, requesterEmail);
        
        // Step 3: Wait for approval/rejection (24 hour timeout)
        logger.info("Waiting for approval decision for request: {}", requestId);
        
        boolean decisionReceived = Workflow.await(
                Duration.ofHours(24),
                () -> approvalReceived || rejectionReceived
        );
        
        if (!decisionReceived) {
            currentStep = "TIMEOUT";
            approvalStatus = "TIMEOUT";
            activities.notifyRequester(requesterEmail, "Request timed out after 24 hours without approval");
            return "Request timed out after 24 hours";
        }
        
        // Process decision
        if (approvalReceived) {
            return processApproval();
        } else if (rejectionReceived) {
            return processRejection();
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
    
    private String processApproval() {
        currentStep = "APPROVED";
        approvalStatus = "APPROVED";
        
        String executionResult = activities.executeApprovedAction(requestId, requestDetails);
        
        activities.notifyRequester(requesterEmail, 
                String.format("Access request approved by %s. Comments: %s", approverEmail, approvalComments));
        activities.logApprovalDecision(requestId, "APPROVED", approverEmail, approvalComments);
        
        currentStep = "COMPLETED";
        return String.format("Access request approved by %s. Result: %s", approverEmail, executionResult);
    }
    
    private String processRejection() {
        currentStep = "REJECTED";
        approvalStatus = "REJECTED";
        
        activities.notifyRequester(requesterEmail, 
                String.format("Access request rejected by %s. Reason: %s", approverEmail, rejectionReason));
        activities.logApprovalDecision(requestId, "REJECTED", approverEmail, rejectionReason);
        
        currentStep = "COMPLETED";
        return String.format("Access request rejected by %s. Reason: %s", approverEmail, rejectionReason);
    }
    
    @Override
    public String getApprovalStatus() {
        return approvalStatus;
    }
    
    @Override
    public String getRequestDetails() {
        return String.format("Request ID: %s, Type: ACCESS_REQUEST, Details: %s, Requester: %s", 
                requestId, requestDetails, requesterEmail);
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