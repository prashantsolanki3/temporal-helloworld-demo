package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.activities.ExternalApiActivitiesImpl;
import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.workflows.OrchestrationWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orchestration")
public class OrchestrationController {
    
    @Autowired
    private WorkflowClient workflowClient;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeOrchestration(@RequestBody OrchestrationRequest request) {
        String workflowId = "orchestration-" + UUID.randomUUID().toString();
        
        // Create workflow stub
        OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                OrchestrationWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build()
        );
        
        // Start workflow asynchronously
        WorkflowClient.start(workflow::orchestrateExternalApiCalls, request.getUserId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("userId", request.getUserId());
        response.put("status", "STARTED");
        response.put("startTime", LocalDateTime.now().format(formatter));
        response.put("message", "Orchestration workflow started successfully. Use workflowId to track progress or get results.");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/execute-sync")
    public ResponseEntity<Map<String, Object>> executeOrchestrationSync(@RequestBody OrchestrationRequest request) {
        String workflowId = "orchestration-sync-" + UUID.randomUUID().toString();
        String startTime = LocalDateTime.now().format(formatter);
        
        try {
            // Create workflow stub
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                    OrchestrationWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(TemporalConfig.TASK_QUEUE)
                            .build()
            );
            
            // Execute workflow synchronously and wait for result
            String result = workflow.orchestrateExternalApiCalls(request.getUserId());
            String endTime = LocalDateTime.now().format(formatter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("userId", request.getUserId());
            response.put("status", "COMPLETED");
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("result", result);
            response.put("message", "Orchestration completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("userId", request.getUserId());
            errorResponse.put("status", "FAILED");
            errorResponse.put("startTime", startTime);
            errorResponse.put("endTime", LocalDateTime.now().format(formatter));
            errorResponse.put("error", "Orchestration failed");
            errorResponse.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/status/{workflowId}")
    public ResponseEntity<Map<String, Object>> getOrchestrationStatus(@PathVariable String workflowId) {
        try {
            // Get workflow stub by ID
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(OrchestrationWorkflow.class, workflowId);
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("checkTime", LocalDateTime.now().format(formatter));
            
            // Check if workflow is completed
            try {
                // Try to get result without blocking (this will throw if workflow is still running)
                String result = workflowStub.getResult(String.class);
                response.put("status", "COMPLETED");
                response.put("completed", true);
                response.put("result", result);
            } catch (Exception e) {
                // Workflow is still running
                response.put("status", "RUNNING");
                response.put("completed", false);
                response.put("message", "Orchestration is still in progress");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow status");
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("details", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/result/{workflowId}")
    public ResponseEntity<Map<String, Object>> getOrchestrationResult(@PathVariable String workflowId) {
        try {
            // Get workflow stub by ID
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(OrchestrationWorkflow.class, workflowId);
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
            
            // Get the final result (this will block until workflow completes)
            String result = workflowStub.getResult(String.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("status", "COMPLETED");
            response.put("completed", true);
            response.put("retrievedTime", LocalDateTime.now().format(formatter));
            response.put("result", result);
            response.put("message", "Orchestration completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get workflow result");
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("details", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Quick test endpoint that doesn't require a request body
    @GetMapping("/test/{userId}")
    public ResponseEntity<Map<String, Object>> testOrchestration(@PathVariable String userId) {
        OrchestrationRequest request = new OrchestrationRequest();
        request.setUserId(userId);
        return executeOrchestrationSync(request);
    }
    
    // Error simulation control endpoints for testing retry functionality
    @PostMapping("/error-simulation/enable")
    public ResponseEntity<Map<String, Object>> enableErrorSimulation() {
        ExternalApiActivitiesImpl.setErrorSimulationEnabled(true);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Error simulation ENABLED - Activities will now randomly throw exceptions to test retry functionality");
        response.put("errorSimulationEnabled", true);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        response.put("retryConfiguration", "5 max attempts, 2s initial interval, 30s max interval, 2.0 backoff coefficient");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/error-simulation/disable")
    public ResponseEntity<Map<String, Object>> disableErrorSimulation() {
        ExternalApiActivitiesImpl.setErrorSimulationEnabled(false);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Error simulation DISABLED - Activities will run normally without random errors");
        response.put("errorSimulationEnabled", false);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/error-simulation/status")
    public ResponseEntity<Map<String, Object>> getErrorSimulationStatus() {
        boolean enabled = ExternalApiActivitiesImpl.isErrorSimulationEnabled();
        Map<String, Object> response = new HashMap<>();
        response.put("errorSimulationEnabled", enabled);
        response.put("status", enabled ? "ENABLED" : "DISABLED");
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        if (enabled) {
            Map<String, Object> errorRates = new HashMap<>();
            errorRates.put("UserService", "25% + 15% post-processing");
            errorRates.put("OrderService", "35%");
            errorRates.put("PaymentService", "40% + 20% validation");
            errorRates.put("NotificationService", "20%");
            errorRates.put("RecommendationService", "30% + 10% ML processing");
            response.put("errorRates", errorRates);
            
            response.put("retryConfiguration", Map.of(
                "maxAttempts", 5,
                "initialInterval", "2 seconds",
                "maxInterval", "30 seconds",
                "backoffCoefficient", 2.0
            ));
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-async-payment")
    public ResponseEntity<Map<String, Object>> testAsyncPayment(@RequestBody OrchestrationRequest request) {
        String workflowId = "async-payment-test-" + UUID.randomUUID().toString();
        String startTime = LocalDateTime.now().format(formatter);
        
        try {
            // Create workflow stub
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                    OrchestrationWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(TemporalConfig.TASK_QUEUE)
                            .build()
            );
            
            // Execute workflow synchronously to see async payment polling in action
            String result = workflow.orchestrateExternalApiCalls(request.getUserId());
            String endTime = LocalDateTime.now().format(formatter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("userId", request.getUserId());
            response.put("status", "COMPLETED");
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("result", result);
            response.put("message", "Async Payment Orchestration completed - check logs for polling activity");
            response.put("paymentPattern", "async-with-polling-every-minute");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("workflowId", workflowId);
            errorResponse.put("userId", request.getUserId());
            errorResponse.put("status", "FAILED");
            errorResponse.put("startTime", startTime);
            errorResponse.put("endTime", LocalDateTime.now().format(formatter));
            errorResponse.put("error", "Async Payment Orchestration failed");
            errorResponse.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // Request DTO
    public static class OrchestrationRequest {
        private String userId;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}