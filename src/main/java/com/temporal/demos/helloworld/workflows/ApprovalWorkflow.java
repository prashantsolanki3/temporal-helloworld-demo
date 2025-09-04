package com.temporal.demos.helloworld.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {
    
    @WorkflowMethod
    String processApprovalRequest(String requestId, String requestType, String requestDetails, String requesterEmail);
    
    @SignalMethod
    void approve(String approverEmail, String comments);
    
    @SignalMethod
    void reject(String approverEmail, String reason);
    
    @QueryMethod
    String getApprovalStatus();
    
    @QueryMethod
    String getRequestDetails();
    
    @QueryMethod
    String getCurrentStep();
    
    @QueryMethod
    long getWaitingTimeInSeconds();
}