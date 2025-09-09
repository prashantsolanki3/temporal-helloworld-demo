package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.activities.ExternalApiActivitiesImpl;
import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.models.OrchestrationRequest;
import com.temporal.demos.helloworld.utils.WorkflowUtil;
import com.temporal.demos.helloworld.workflows.OrchestrationWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return WorkflowUtil.getWorkflowStatus(workflowClient, workflowId);
    }

    @GetMapping("/result/{workflowId}")
    public ResponseEntity<Map<String, Object>> getOrchestrationResult(@PathVariable String workflowId) {
        return WorkflowUtil.getWorkflowResult(workflowClient, workflowId, response -> {
            // Add orchestration-specific data - set status to COMPLETED for consistency
            response.put("status", "COMPLETED");
        });
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