package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.activities.ExternalApiActivitiesImpl;
import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.models.OrchestrationRequest;
import com.temporal.demos.helloworld.workflows.OrchestrationWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
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
@RequestMapping("/api/orchestration")
public class OrchestrationController {

    @Autowired
    private WorkflowClient workflowClient;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeOrchestration(@RequestBody OrchestrationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        String stableKey = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : request.getRequestId();
        String workflowId = "approval-" + stableKey;

        OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                OrchestrationWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build());
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("userId", request.getUserId());

        try {
            WorkflowClient.start(workflow::orchestrateExternalApiCalls, request.getUserId(),
                    request.isUseAsyncPayment());
            response.put("status", "STARTED");
            response.put("message", "Orchestration workflow started successfully");
        } catch (WorkflowExecutionAlreadyStarted e) {
            // existing workflow; request is a retry
            response.put("status", "ALREADY_EXISTS");
            response.put("message", "Orchestration workflow already exists");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute-sync")
    public ResponseEntity<Map<String, Object>> executeOrchestrationSync(@RequestBody OrchestrationRequest request) {
        String workflowId = "orchestration-sync-" + UUID.randomUUID().toString();

        try {
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                    OrchestrationWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(TemporalConfig.TASK_QUEUE)
                            .build());

            String result = workflow.orchestrateExternalApiCalls(request.getUserId(), request.isUseAsyncPayment());

            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("userId", request.getUserId());
            response.put("status", "COMPLETED");
            response.put("result", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/status/{workflowId}")
    public ResponseEntity<Map<String, Object>> getOrchestrationStatus(@PathVariable String workflowId) {
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
                        workflowExecutionInfo.getStartTime().getNanos()).toString());
            }

            if (workflowExecutionInfo.hasCloseTime()) {
                statusResponse.put("closeTime", Instant.ofEpochSecond(
                        workflowExecutionInfo.getCloseTime().getSeconds(),
                        workflowExecutionInfo.getCloseTime().getNanos()).toString());
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
    public ResponseEntity<Map<String, Object>> getOrchestrationResult(@PathVariable String workflowId) {
        try {
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(OrchestrationWorkflow.class, workflowId);
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);

            String result = workflowStub.getResult(String.class);

            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("status", "COMPLETED");
            response.put("result", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow result");
            errorResponse.put("workflowId", workflowId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/error-simulation/enable")
    public ResponseEntity<Map<String, Object>> enableErrorSimulation() {
        ExternalApiActivitiesImpl.setErrorSimulationEnabled(true);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Error simulation ENABLED");
        response.put("errorSimulationEnabled", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/error-simulation/disable")
    public ResponseEntity<Map<String, Object>> disableErrorSimulation() {
        ExternalApiActivitiesImpl.setErrorSimulationEnabled(false);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Error simulation DISABLED");
        response.put("errorSimulationEnabled", false);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/error-simulation/status")
    public ResponseEntity<Map<String, Object>> getErrorSimulationStatus() {
        boolean enabled = ExternalApiActivitiesImpl.isErrorSimulationEnabled();
        Map<String, Object> response = new HashMap<>();
        response.put("errorSimulationEnabled", enabled);
        response.put("status", enabled ? "ENABLED" : "DISABLED");
        return ResponseEntity.ok(response);
    }

}