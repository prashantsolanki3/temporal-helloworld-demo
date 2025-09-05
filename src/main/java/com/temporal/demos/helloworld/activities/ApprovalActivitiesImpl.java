package com.temporal.demos.helloworld.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class ApprovalActivitiesImpl implements ApprovalActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(ApprovalActivitiesImpl.class);
    private static final List<String> VALID_REQUEST_TYPES = Arrays.asList(
            "BUDGET_REQUEST", "PERSONNEL_CHANGE", "SYSTEM_ACCESS", "PROCUREMENT", "POLICY_EXCEPTION"
    );
    
    @Override
    public String validateRequest(String requestId, String requestType, String requestDetails) {
        logger.info("Validating request: {} of type: {}", requestId, requestType);
        
        if (requestId == null || requestId.trim().isEmpty()) {
            return "Invalid request ID";
        }
        
        if (!VALID_REQUEST_TYPES.contains(requestType)) {
            return "Invalid request type. Valid types: " + String.join(", ", VALID_REQUEST_TYPES);
        }
        
        if (requestDetails == null || requestDetails.trim().length() < 10) {
            return "Request details must be at least 10 characters long";
        }
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Request {} validated successfully", requestId);
        return "VALID";
    }
    
    @Override
    public void notifyApprovers(String requestId, String requestType, String requestDetails, String requesterEmail) {
        logger.info("Notifying approvers for request: {} from: {}", requestId, requesterEmail);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String message = String.format("NEW APPROVAL REQUEST - ID: %s, Type: %s, Requester: %s, Submitted: %s", 
                requestId, requestType, requesterEmail, timestamp);
        
        logger.info("Approval notification sent: {}", message);
        
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
    public String executeApprovedAction(String requestId, String requestType, String requestDetails) {
        logger.info("Executing approved action for request: {} of type: {}", requestId, requestType);
        
        String result;
        Random random = new Random();
        
        try {
            switch (requestType) {
                case "BUDGET_REQUEST":
                    Thread.sleep(3000);
                    result = "Budget allocated. Reference: BUDGET-" + System.currentTimeMillis();
                    break;
                case "PERSONNEL_CHANGE":
                    Thread.sleep(2000);
                    result = "Personnel record updated. Employee ID: EMP-" + random.nextInt(10000);
                    break;
                case "SYSTEM_ACCESS":
                    Thread.sleep(1500);
                    result = "System access granted. Access token: TOK-" + random.nextInt(100000);
                    break;
                case "PROCUREMENT":
                    Thread.sleep(4000);
                    result = "Purchase order created. PO Number: PO-" + System.currentTimeMillis();
                    break;
                case "POLICY_EXCEPTION":
                    Thread.sleep(1000);
                    result = "Exception granted. Exception ID: EXC-" + random.nextInt(1000);
                    break;
                default:
                    result = "Action executed with generic handler";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = "Execution interrupted";
        }
        
        logger.info("Action executed for request: {} with result: {}", requestId, result);
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