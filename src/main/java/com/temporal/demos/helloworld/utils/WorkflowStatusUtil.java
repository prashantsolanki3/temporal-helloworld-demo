package com.temporal.demos.helloworld.utils;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class for checking workflow status asynchronously.
 * Provides common functionality for workflow status endpoints.
 */
public class WorkflowStatusUtil {

    /**
     * Gets workflow status information asynchronously without blocking.
     * 
     * @param workflowClient The Temporal workflow client
     * @param workflowId The workflow ID to check
     * @param runningWorkflowHandler Optional handler for additional queries when workflow is running
     * @return ResponseEntity with workflow status information
     */
    public static ResponseEntity<Map<String, Object>> getWorkflowStatus(
            WorkflowClient workflowClient, 
            String workflowId,
            Consumer<Map<String, Object>> runningWorkflowHandler) {
        
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

            // Handle running workflows with custom handler if provided
            if (executionStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING 
                && runningWorkflowHandler != null) {
                try {
                    runningWorkflowHandler.accept(statusResponse);
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

    /**
     * Overloaded method for simple workflow status checking without custom running workflow handling.
     * 
     * @param workflowClient The Temporal workflow client
     * @param workflowId The workflow ID to check
     * @return ResponseEntity with workflow status information
     */
    public static ResponseEntity<Map<String, Object>> getWorkflowStatus(
            WorkflowClient workflowClient, 
            String workflowId) {
        return getWorkflowStatus(workflowClient, workflowId, null);
    }
}