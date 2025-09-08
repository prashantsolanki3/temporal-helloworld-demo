package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.activities.ExternalApiActivitiesImpl;
import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.models.OrchestrationRequest;
import com.temporal.demos.helloworld.workflows.OrchestrationWorkflow;
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
@RequestMapping("/api/orchestration")
public class OrchestrationController {
    
    @Autowired
    private WorkflowClient workflowClient;
    
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeOrchestration(@RequestBody OrchestrationRequest request) {
        String workflowId = "orchestration-" + UUID.randomUUID().toString();
        
        OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(
                OrchestrationWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build()
        );
        
        WorkflowClient.start(workflow::orchestrateExternalApiCalls, request.getUserId(), request.isUseAsyncPayment());
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("userId", request.getUserId());
        response.put("status", "STARTED");
        response.put("message", "Orchestration workflow started successfully");
        
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
                            .build()
            );
            
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
            OrchestrationWorkflow workflow = workflowClient.newWorkflowStub(OrchestrationWorkflow.class, workflowId);
            WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            
            try {
                String result = workflowStub.getResult(String.class);
                response.put("status", "COMPLETED");
                response.put("completed", true);
                response.put("result", result);
            } catch (Exception e) {
                response.put("status", "RUNNING");
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
    
    // Quick test endpoint that doesn't require a request body
    @GetMapping("/test/{userId}")
    public ResponseEntity<Map<String, Object>> testOrchestration(@PathVariable String userId) {
        OrchestrationRequest request = new OrchestrationRequest(userId, false);
        return executeOrchestrationSync(request);
    }
    
    @GetMapping("/test-async/{userId}")
    public ResponseEntity<Map<String, Object>> testAsyncOrchestration(@PathVariable String userId) {
        OrchestrationRequest request = new OrchestrationRequest(userId, true);
        return executeOrchestrationSync(request);
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