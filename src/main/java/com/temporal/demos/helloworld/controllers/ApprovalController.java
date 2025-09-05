package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.workflows.ApprovalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {
    
    @Autowired
    private WorkflowClient workflowClient;
    
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> submitApprovalRequest(@RequestBody ApprovalRequest request) {
        String workflowId = "approval-" + UUID.randomUUID().toString();
        
        ApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                ApprovalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build()
        );
        
        WorkflowClient.start(workflow::processApprovalRequest, 
                request.getRequestId(), 
                request.getRequestType(), 
                request.getRequestDetails(), 
                request.getRequesterEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("requestId", request.getRequestId());
        response.put("status", "SUBMITTED");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/approve/{workflowId}")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable String workflowId,
            @RequestBody ApprovalDecision decision) {
        
        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
            workflow.approve(decision.getApproverEmail(), decision.getComments());
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("action", "APPROVED");
            response.put("approver", decision.getApproverEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send approval signal");
            errorResponse.put("workflowId", workflowId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/reject/{workflowId}")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable String workflowId,
            @RequestBody ApprovalDecision decision) {
        
        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
            workflow.reject(decision.getApproverEmail(), decision.getReason());
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("action", "REJECTED");
            response.put("approver", decision.getApproverEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send rejection signal");
            errorResponse.put("workflowId", workflowId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/status/{workflowId}")
    public ResponseEntity<Map<String, Object>> getApprovalStatus(@PathVariable String workflowId) {
        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
            
            String status = workflow.getApprovalStatus();
            String currentStep = workflow.getCurrentStep();
            long waitingTime = workflow.getWaitingTimeInSeconds();
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("status", status);
            response.put("currentStep", currentStep);
            response.put("waitingTimeSeconds", waitingTime);
            
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
            try {
                String result = workflowStub.getResult(String.class);
                response.put("completed", true);
                response.put("result", result);
            } catch (Exception e) {
                response.put("completed", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow status");
            errorResponse.put("workflowId", workflowId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/result/{workflowId}")
    public ResponseEntity<Map<String, Object>> getApprovalResult(@PathVariable String workflowId) {
        try {
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
            
            String result = workflowStub.getResult(String.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("completed", true);
            response.put("result", result);
            response.put("finalStatus", workflow.getApprovalStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow result");
            errorResponse.put("workflowId", workflowId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Request DTOs
    public static class ApprovalRequest {
        private String requestId;
        private String requestType;
        private String requestDetails;
        private String requesterEmail;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        
        public String getRequestDetails() { return requestDetails; }
        public void setRequestDetails(String requestDetails) { this.requestDetails = requestDetails; }
        
        public String getRequesterEmail() { return requesterEmail; }
        public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    }
    
    public static class ApprovalDecision {
        private String approverEmail;
        private String comments;
        private String reason;
        
        // Getters and setters
        public String getApproverEmail() { return approverEmail; }
        public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}