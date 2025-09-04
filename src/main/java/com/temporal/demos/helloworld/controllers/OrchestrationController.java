package com.temporal.demos.helloworld.controllers;

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
    
    // Request DTO
    public static class OrchestrationRequest {
        private String userId;
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}