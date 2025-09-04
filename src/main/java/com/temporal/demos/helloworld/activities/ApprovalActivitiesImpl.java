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
        
        // Simulate validation logic
        if (requestId == null || requestId.trim().isEmpty()) {
            return "Invalid request ID";
        }
        
        if (!VALID_REQUEST_TYPES.contains(requestType)) {
            return "Invalid request type. Valid types: " + String.join(", ", VALID_REQUEST_TYPES);
        }
        
        if (requestDetails == null || requestDetails.trim().length() < 10) {
            return "Request details must be at least 10 characters long";
        }
        
        // Simulate some processing time
        try {
            Thread.sleep(2000); // 2 seconds validation time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Request {} validated successfully", requestId);
        return "VALID";
    }
    
    @Override
    public void notifyApprovers(String requestId, String requestType, String requestDetails, String requesterEmail) {
        logger.info("Notifying approvers for request: {} from: {}", requestId, requesterEmail);
        
        // Simulate email/notification sending
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String notificationMessage = String.format(
                "NEW APPROVAL REQUEST\n" +
                "==================\n" +
                "Request ID: %s\n" +
                "Type: %s\n" +
                "Requester: %s\n" +
                "Details: %s\n" +
                "Submitted: %s\n\n" +
                "Please review and approve/reject via the approval system.\n" +
                "Timeout: 24 hours",
                requestId, requestType, requesterEmail, requestDetails, timestamp
        );
        
        // In a real system, this would send emails, Slack messages, etc.
        logger.info("Approval notification sent:\n{}", notificationMessage);
        
        // Simulate notification delivery time
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
        String fullMessage = String.format(
                "APPROVAL REQUEST UPDATE\n" +
                "======================\n" +
                "Time: %s\n" +
                "Message: %s\n",
                timestamp, message
        );
        
        // In a real system, this would send email, SMS, push notification, etc.
        logger.info("Requester notification sent:\n{}", fullMessage);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public String executeApprovedAction(String requestId, String requestType, String requestDetails) {
        logger.info("Executing approved action for request: {} of type: {}", requestId, requestType);
        
        // Simulate different execution based on request type
        String result;
        Random random = new Random();
        
        try {
            switch (requestType) {
                case "BUDGET_REQUEST":
                    Thread.sleep(3000); // Simulate budget allocation
                    result = "Budget allocated. Reference: BUDGET-" + System.currentTimeMillis();
                    break;
                    
                case "PERSONNEL_CHANGE":
                    Thread.sleep(2000); // Simulate HR system update
                    result = "Personnel record updated. Employee ID: EMP-" + random.nextInt(10000);
                    break;
                    
                case "SYSTEM_ACCESS":
                    Thread.sleep(1500); // Simulate access provisioning
                    result = "System access granted. Access token: TOK-" + random.nextInt(100000);
                    break;
                    
                case "PROCUREMENT":
                    Thread.sleep(4000); // Simulate procurement process
                    result = "Purchase order created. PO Number: PO-" + System.currentTimeMillis();
                    break;
                    
                case "POLICY_EXCEPTION":
                    Thread.sleep(1000); // Simulate policy update
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
        String logEntry = String.format(
                "APPROVAL DECISION LOG\n" +
                "====================\n" +
                "Request ID: %s\n" +
                "Decision: %s\n" +
                "Approver: %s\n" +
                "Comments/Reason: %s\n" +
                "Timestamp: %s\n",
                requestId, decision, approverEmail, comments, timestamp
        );
        
        // In a real system, this would write to audit log, database, etc.
        logger.info("Decision logged:\n{}", logEntry);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}