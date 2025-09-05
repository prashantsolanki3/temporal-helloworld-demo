package com.temporal.demos.helloworld.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class ApprovalActivitiesImpl implements ApprovalActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(ApprovalActivitiesImpl.class);
    
    @Override
    public String validateRequest(String requestId, String requestDetails) {
        logger.info("Validating access request: {}", requestId);
        
        if (requestId == null || requestId.trim().isEmpty()) {
            return "Invalid request ID";
        }
        
        if (requestDetails == null || requestDetails.trim().length() < 10) {
            return "Request details must be at least 10 characters long";
        }
        
        // Simulate validation processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Access request {} validated successfully", requestId);
        return "VALID";
    }
    
    @Override
    public void notifyApprovers(String requestId, String requestDetails, String requesterEmail) {
        logger.info("Notifying approvers for access request: {} from: {}", requestId, requesterEmail);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String message = String.format("NEW ACCESS REQUEST - ID: %s, Requester: %s, Details: %s, Submitted: %s", 
                requestId, requesterEmail, requestDetails, timestamp);
        
        logger.info("Approval notification sent: {}", message);
        
        // Simulate notification processing time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void notifyRequester(String requesterEmail, String message) {
        logger.info("Notifying requester: {} with message: {}", requesterEmail, message);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String fullMessage = String.format("APPROVAL UPDATE [%s]: %s", timestamp, message);
        
        logger.info("Requester notification sent: {}", fullMessage);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public String executeApprovedAction(String requestId, String requestDetails) {
        logger.info("Executing approved access request: {}", requestId);
        
        // Simulate access provisioning
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Execution interrupted";
        }
        
        Random random = new Random();
        String result = "Access granted. Access token: TOK-" + random.nextInt(100000);
        
        logger.info("Access granted for request: {} with result: {}", requestId, result);
        return result;
    }
    
    @Override
    public void logApprovalDecision(String requestId, String decision, String approverEmail, String comments) {
        logger.info("Logging approval decision for request: {}", requestId);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logEntry = String.format("DECISION LOG - Request: %s, Decision: %s, Approver: %s, Comments: %s, Time: %s",
                requestId, decision, approverEmail, comments, timestamp);
        
        logger.info("Decision logged: {}", logEntry);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}