package com.temporal.demos.helloworld.workflows;

import com.temporal.demos.helloworld.activities.ExternalApiActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrchestrationWorkflowImpl implements OrchestrationWorkflow {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Configure activity options with timeout and retry policy
    // Enhanced retry configuration for testing random failures
    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(45))  // Increased timeout for retries
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))     // Start with 2 second delay
                    .setMaximumInterval(Duration.ofSeconds(30))    // Max 30 seconds between retries
                    .setBackoffCoefficient(2.0)                    // Double the interval each retry
                    .setMaximumAttempts(5)                         // Increased to 5 attempts to handle errors
                    .build())
            .build();
    
    private final ExternalApiActivities activities = 
            Workflow.newActivityStub(ExternalApiActivities.class, activityOptions);
    
    @Override
    public String orchestrateExternalApiCalls(String userId) {
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Starting orchestration for user: {} (with retry-enabled activities)", userId);
        
        String startTime = LocalDateTime.now().format(formatter);
        
        // STEP 1: Start with UserService (must run first to get user data)
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Step 1: Calling UserService first (critical service with retries)...");
        String userServiceResult;
        try {
            userServiceResult = activities.callUserService(userId);
            Workflow.getLogger(OrchestrationWorkflowImpl.class).info("UserService completed successfully for user: {}", userId);
        } catch (Exception e) {
            Workflow.getLogger(OrchestrationWorkflowImpl.class).error("UserService failed after all retries for user: {}", userId, e);
            throw e;
        }
        
        // STEP 2: After user data is retrieved, start parallel calls for independent services
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Step 2: Starting parallel calls for independent services (with retries)...");
        Promise<String> orderServicePromise = Async.function(activities::callOrderService, userId);
        Promise<String> notificationServicePromise = Async.function(activities::callNotificationService, userId);
        
        // STEP 3: PaymentService runs sequentially after UserService (simulates dependency on user verification)
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Step 3: Calling PaymentService after user verification (with retries)...");
        String paymentServiceResult;
        try {
            paymentServiceResult = activities.callPaymentService(userId);
            Workflow.getLogger(OrchestrationWorkflowImpl.class).info("PaymentService completed successfully for user: {}", userId);
        } catch (Exception e) {
            Workflow.getLogger(OrchestrationWorkflowImpl.class).error("PaymentService failed after all retries for user: {}", userId, e);
            throw e;
        }
        
        // STEP 4: Wait for the parallel services to complete
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Step 4: Waiting for parallel services to complete (with retries)...");
        String orderServiceResult;
        String notificationServiceResult;
        try {
            orderServiceResult = orderServicePromise.get();
            Workflow.getLogger(OrchestrationWorkflowImpl.class).info("OrderService completed successfully for user: {}", userId);
        } catch (Exception e) {
            Workflow.getLogger(OrchestrationWorkflowImpl.class).error("OrderService failed after all retries for user: {}", userId, e);
            throw e;
        }
        
        try {
            notificationServiceResult = notificationServicePromise.get();
            Workflow.getLogger(OrchestrationWorkflowImpl.class).info("NotificationService completed successfully for user: {}", userId);
        } catch (Exception e) {
            Workflow.getLogger(OrchestrationWorkflowImpl.class).error("NotificationService failed after all retries for user: {}", userId, e);
            throw e;
        }
        
        // STEP 5: RecommendationService runs last (depends on user, order, and payment data)
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Step 5: Calling RecommendationService with all data available (with retries)...");
        String recommendationServiceResult;
        try {
            recommendationServiceResult = activities.callRecommendationService(userId);
            Workflow.getLogger(OrchestrationWorkflowImpl.class).info("RecommendationService completed successfully for user: {}", userId);
        } catch (Exception e) {
            Workflow.getLogger(OrchestrationWorkflowImpl.class).error("RecommendationService failed after all retries for user: {}", userId, e);
            throw e;
        }
        
        String endTime = LocalDateTime.now().format(formatter);
        
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("All API calls completed for user: {}", userId);
        
        // Collect all results in order
        List<String> allResults = new ArrayList<>();
        allResults.add(userServiceResult);
        allResults.add(orderServiceResult);
        allResults.add(paymentServiceResult);
        allResults.add(notificationServiceResult);
        allResults.add(recommendationServiceResult);
        
        // Compile results into a single response
        StringBuilder compiledResult = new StringBuilder();
        compiledResult.append("{");
        compiledResult.append("\"orchestrationResult\":{");
        compiledResult.append("\"userId\":\"").append(userId).append("\",");
        compiledResult.append("\"startTime\":\"").append(startTime).append("\",");
        compiledResult.append("\"endTime\":\"").append(endTime).append("\",");
        compiledResult.append("\"totalServices\":").append(allResults.size()).append(",");
        compiledResult.append("\"executionPattern\":\"mixed-sequential-parallel\",");
        compiledResult.append("\"executionSteps\":[");
        compiledResult.append("\"1. UserService (sequential - first)\",");
        compiledResult.append("\"2. OrderService + NotificationService (parallel)\",");
        compiledResult.append("\"3. PaymentService (sequential - after user)\",");
        compiledResult.append("\"4. RecommendationService (sequential - last)\"");
        compiledResult.append("],");
        compiledResult.append("\"services\":[");
        
        // Get results from all services
        for (int i = 0; i < allResults.size(); i++) {
            if (i > 0) {
                compiledResult.append(",");
            }
            compiledResult.append(allResults.get(i));
        }
        
        compiledResult.append("]");
        compiledResult.append("}");
        compiledResult.append("}");
        
        String finalResult = compiledResult.toString();
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Orchestration completed for user: {} with compiled result length: {}", userId, finalResult.length());
        
        return finalResult;
    }
}