package com.temporal.demos.helloworld.workflows;

import com.temporal.demos.helloworld.activities.ExternalApiActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OrchestrationWorkflowImpl implements OrchestrationWorkflow {
    
    // Activity options with retry configuration
    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(45))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(5)
                    .build())
            .build();
    
    // Polling configuration for async payments
    private final ActivityOptions pollingActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(10)
                    .build())
            .build();
    
    private final ExternalApiActivities activities = 
            Workflow.newActivityStub(ExternalApiActivities.class, activityOptions);
    
    private final ExternalApiActivities pollingActivities = 
            Workflow.newActivityStub(ExternalApiActivities.class, pollingActivityOptions);
    
    @Override
    public String orchestrateExternalApiCalls(String userId, boolean useAsyncPayment) {
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Starting orchestration for user: {} (async payment: {})", userId, useAsyncPayment);
        
        // Step 1: UserService (runs first)
        String userServiceResult = activities.callUserService(userId);
        
        // Step 2: Async Parallel services
        Promise<String> orderServicePromise = Async.function(activities::callOrderService, userId);
        Promise<String> notificationServicePromise = Async.function(activities::callNotificationService, userId);
        
        // Step 3: PaymentService
        String paymentServiceResult;
        if (useAsyncPayment) {
            // Async payment with polling, this can also be done with Promise and Async
            // to demonstrate different patterns
            String paymentInitResult = activities.initiateAsyncPaymentProcess(userId, 150.75);
            String paymentId = extractPaymentId(paymentInitResult);
            paymentServiceResult = pollingActivities.pollPaymentStatus(paymentId);
        } else {
            // Synchronous payment
            paymentServiceResult = activities.callPaymentService(userId);
        }
        
        // Step 4: Wait for parallel services to complete
        String orderServiceResult = orderServicePromise.get();
        String notificationServiceResult = notificationServicePromise.get();
        
        // Step 5: RecommendationService - Starts after the parallel services complete.
        String recommendationServiceResult = activities.callRecommendationService(userId);
        
        Workflow.getLogger(OrchestrationWorkflowImpl.class).info("Orchestration completed for user: {}", userId);
        
        return compileResults(userId, useAsyncPayment, userServiceResult, orderServiceResult, 
                paymentServiceResult, notificationServiceResult, recommendationServiceResult);
    }
    
    private String compileResults(String userId, boolean useAsyncPayment, String userServiceResult, 
                                 String orderServiceResult, String paymentServiceResult, 
                                 String notificationServiceResult, String recommendationServiceResult) {
        List<String> allResults = new ArrayList<>();
        allResults.add(userServiceResult);
        allResults.add(orderServiceResult);
        allResults.add(paymentServiceResult);
        allResults.add(notificationServiceResult);
        allResults.add(recommendationServiceResult);
        
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"orchestrationResult\":{");
        result.append("\"userId\":\"").append(userId).append("\",");
        result.append("\"totalServices\":").append(allResults.size()).append(",");
        result.append("\"paymentMode\":\"").append(useAsyncPayment ? "async" : "sync").append("\",");
        result.append("\"services\":[");
        
        for (int i = 0; i < allResults.size(); i++) {
            if (i > 0) result.append(",");
            result.append(allResults.get(i));
        }
        
        result.append("]}");
        result.append("}");
        
        return result.toString();
    }
    
    private String extractPaymentId(String jsonResult) {
        // Simple string parsing for demo purposes
        int startIndex = jsonResult.indexOf("\"paymentId\":\"") + 13;
        int endIndex = jsonResult.indexOf("\"", startIndex);
        return jsonResult.substring(startIndex, endIndex);
    }
}