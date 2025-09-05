package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.workflows.ApprovalWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
                "ACCESS_REQUEST", // Fixed type for demo
                request.getRequestDetails(), 
                request.getRequesterEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("requestId", request.getRequestId());
        response.put("requestType", "ACCESS_REQUEST");
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
            // Create workflow execution reference
            WorkflowExecution execution = WorkflowExecution.newBuilder()
                    .setWorkflowId(workflowId)
                    .build();
            
            // Create describe request
            DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
                    .setNamespace(workflowClient.getOptions().getNamespace())
                    .setExecution(execution)
                    .build();
            
            // Get workflow service stubs
            WorkflowServiceStubs serviceStubs = workflowClient.getWorkflowServiceStubs();
            
            // Describe workflow execution
            DescribeWorkflowExecutionResponse response = serviceStubs.blockingStub()
                    .describeWorkflowExecution(request);
            
            // Get workflow execution info
            var workflowExecutionInfo = response.getWorkflowExecutionInfo();
            WorkflowExecutionStatus executionStatus = workflowExecutionInfo.getStatus();
            
            Map<String, Object> statusResponse = new HashMap<>();
            statusResponse.put("workflowId", workflowId);
            statusResponse.put("executionStatus", executionStatus.name());
            statusResponse.put("workflowType", workflowExecutionInfo.getType().getName());
            
            // Add timing information
            if (workflowExecutionInfo.hasStartTime()) {
                statusResponse.put("startTime", Instant.ofEpochSecond(
                    workflowExecutionInfo.getStartTime().getSeconds(),
                    workflowExecutionInfo.getStartTime().getNanos()
                ).toString());
            }
            
            if (workflowExecutionInfo.hasCloseTime()) {
                statusResponse.put("closeTime", Instant.ofEpochSecond(
                    workflowExecutionInfo.getCloseTime().getSeconds(),
                    workflowExecutionInfo.getCloseTime().getNanos()
                ).toString());
            }
            
            // Only query custom status if workflow is running
            if (executionStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
                try {
                    ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
                    statusResponse.put("approvalStatus", workflow.getApprovalStatus());
                    statusResponse.put("currentStep", workflow.getCurrentStep());
                    statusResponse.put("waitingTimeSeconds", workflow.getWaitingTimeInSeconds());
                    statusResponse.put("requestDetails", workflow.getRequestDetails());
                } catch (Exception e) {
                    // If queries fail, still return execution status
                    statusResponse.put("queryError", "Unable to query workflow details: " + e.getMessage());
                }
            }
            
            // Add result if workflow is completed
            if (executionStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
                try {
                    WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
                    String result = workflowStub.getResult(String.class);
                    statusResponse.put("result", result);
                    statusResponse.put("completed", true);
                } catch (Exception e) {
                    statusResponse.put("resultError", "Unable to get workflow result: " + e.getMessage());
                }
            } else {
                statusResponse.put("completed", false);
            }
            
            // Add failure information if workflow failed
            if (executionStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED) {
                try {
                    WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
                    workflowStub.getResult(String.class);
                } catch (Exception failureException) {
                    statusResponse.put("failure", failureException.getMessage());
                }
            }
            
            return ResponseEntity.ok(statusResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow status: " + e.getMessage());
            errorResponse.put("workflowId", workflowId);
            
            // Check if it's a workflow not found error
            if (e.getMessage().contains("not found") || e.getMessage().contains("NotFound")) {
                errorResponse.put("errorType", "WORKFLOW_NOT_FOUND");
                return ResponseEntity.notFound().build();
            }
            
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
        private String requestDetails;
        private String requesterEmail;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
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