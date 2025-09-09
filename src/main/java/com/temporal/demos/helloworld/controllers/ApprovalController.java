package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.models.ApprovalDecision;
import com.temporal.demos.helloworld.models.ApprovalRequest;
import com.temporal.demos.helloworld.utils.WorkflowStatusUtil;
import com.temporal.demos.helloworld.workflows.ApprovalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private WorkflowClient workflowClient;

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> submitApprovalRequest(@RequestBody ApprovalRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        String stableKey = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : request.getRequestId();
        String workflowId = "approval-" + stableKey;

        ApprovalWorkflow workflow = workflowClient.newWorkflowStub(
                ApprovalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build());

        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("requestId", request.getRequestId());
        response.put("requestType", "ACCESS_REQUEST");
        // Async Execution
        try {
            WorkflowClient.start(workflow::processApprovalRequest,
                    request.getRequestId(),
                    "ACCESS_REQUEST", // Fixed type for demo
                    request.getRequestDetails(),
                    request.getRequesterEmail());
            response.put("status", "SUBMITTED");
        } catch (WorkflowExecutionAlreadyStarted e) {
            // existing workflow; request is a retry
            response.put("status", "ALREADY_EXISTS");
        }

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
        return WorkflowStatusUtil.getWorkflowStatus(workflowClient, workflowId, statusResponse -> {
            // Add approval-specific queries for running workflows
            ApprovalWorkflow workflow = workflowClient.newWorkflowStub(ApprovalWorkflow.class, workflowId);
            statusResponse.put("approvalStatus", workflow.getApprovalStatus());
            statusResponse.put("currentStep", workflow.getCurrentStep());
            statusResponse.put("requestDetails", workflow.getRequestDetails());
        });
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
}