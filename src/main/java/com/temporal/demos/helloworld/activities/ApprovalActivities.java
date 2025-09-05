package com.temporal.demos.helloworld.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ApprovalActivities {
    
    @ActivityMethod
    String validateRequest(String requestId, String requestDetails);
    
    @ActivityMethod
    void notifyApprovers(String requestId, String requestDetails, String requesterEmail);
    
    @ActivityMethod
    void notifyRequester(String requesterEmail, String message);
    
    @ActivityMethod
    String executeApprovedAction(String requestId, String requestDetails);
    
    @ActivityMethod
    void logApprovalDecision(String requestId, String decision, String approverEmail, String comments);
}