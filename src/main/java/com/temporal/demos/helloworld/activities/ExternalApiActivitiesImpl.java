package com.temporal.demos.helloworld.activities;

import com.temporal.demos.helloworld.utils.RandomErrorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExternalApiActivitiesImpl implements ExternalApiActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalApiActivitiesImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Flag to enable/disable random error simulation (can be toggled for testing)
    private static boolean errorSimulationEnabled = false;
    
    @Override
    public String callUserService(String userId) {
        String serviceName = "UserService";
        logger.info("Starting {} call for user: {}", serviceName, userId);
        
        // Simulate potential failure before API call (35% error rate for critical service)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(35, serviceName);
        }
        
        // Simulate API call with 2-4 seconds delay
        simulateApiCall(2000, 4000);
        
        // Check for potential failure after processing (lower rate)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(25, serviceName + "-PostProcessing");
        }
        
        String result = String.format("{\"service\":\"%s\",\"userId\":\"%s\",\"data\":{\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"status\":\"active\"},\"timestamp\":\"%s\",\"duration\":\"2-4s\",\"executionOrder\":\"1-sequential-first\"}", 
                serviceName, userId, LocalDateTime.now().format(formatter));
        
        logger.info("Completed {} call for user: {} with result: {}", serviceName, userId, result);
        return result;
    }
    
    @Override
    public String callOrderService(String userId) {
        String serviceName = "OrderService";
        logger.info("Starting {} call for user: {}", serviceName, userId);
        
        // Higher error rate for this service to test parallel execution retries (45%)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(45, serviceName);
        }
        
        // Simulate API call with 1-3 seconds delay
        simulateApiCall(1000, 3000);
        
        String result = String.format("{\"service\":\"%s\",\"userId\":\"%s\",\"data\":{\"totalOrders\":5,\"lastOrder\":\"2024-01-15\",\"totalAmount\":1250.50},\"timestamp\":\"%s\",\"duration\":\"1-3s\",\"executionOrder\":\"2-parallel-with-notifications\"}", 
                serviceName, userId, LocalDateTime.now().format(formatter));
        
        logger.info("Completed {} call for user: {} with result: {}", serviceName, userId, result);
        return result;
    }
    
    @Override
    public String callPaymentService(String userId) {
        String serviceName = "PaymentService";
        logger.info("Starting {} call for user: {}", serviceName, userId);
        
        // Use the utility method to simulate transient errors during processing (50% error rate)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.simulateTransientError(50, serviceName, 1000, 2000);
        }
        
        // Simulate API call with 3-6 seconds delay (slower payment service)
        simulateApiCall(3000, 6000);
        
        // Additional chance of failure after processing (30%)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(30, serviceName + "-Validation");
        }
        
        String result = String.format("{\"service\":\"%s\",\"userId\":\"%s\",\"data\":{\"paymentMethods\":[\"**** 1234\",\"**** 5678\"],\"defaultMethod\":\"**** 1234\",\"creditScore\":750},\"timestamp\":\"%s\",\"duration\":\"3-6s\",\"executionOrder\":\"3-sequential-after-user\"}", 
                serviceName, userId, LocalDateTime.now().format(formatter));
        
        logger.info("Completed {} call for user: {} with result: {}", serviceName, userId, result);
        return result;
    }
    
    @Override
    public String callNotificationService(String userId) {
        String serviceName = "NotificationService";
        logger.info("Starting {} call for user: {}", serviceName, userId);
        
        // Lower error rate for notifications (30%) as it's less critical but runs in parallel
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(30, serviceName);
        }
        
        // Simulate API call with 0.5-2 seconds delay (fast service)
        simulateApiCall(500, 2000);
        
        String result = String.format("{\"service\":\"%s\",\"userId\":\"%s\",\"data\":{\"unreadCount\":3,\"preferences\":{\"email\":true,\"sms\":false,\"push\":true}},\"timestamp\":\"%s\",\"duration\":\"0.5-2s\",\"executionOrder\":\"2-parallel-with-orders\"}", 
                serviceName, userId, LocalDateTime.now().format(formatter));
        
        logger.info("Completed {} call for user: {} with result: {}", serviceName, userId, result);
        return result;
    }
    
    @Override
    public String callRecommendationService(String userId) {
        String serviceName = "RecommendationService";
        logger.info("Starting {} call for user: {}", serviceName, userId);
        
        // Medium error rate for ML service (40%) as it's complex but runs at the end
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(40, serviceName);
        }
        
        // Simulate API call with 4-8 seconds delay (ML-based service, slower)
        simulateApiCall(4000, 8000);
        
        // Additional potential failure during ML processing
        if (errorSimulationEnabled && RandomErrorGenerator.shouldThrowError(10)) {
            throw RandomErrorGenerator.createServiceException(serviceName, userId, "ML_PROCESSING");
        }
        
        String result = String.format("{\"service\":\"%s\",\"userId\":\"%s\",\"data\":{\"recommendations\":[\"Product A\",\"Product B\",\"Product C\"],\"confidence\":0.85,\"algorithm\":\"collaborative-filtering\"},\"timestamp\":\"%s\",\"duration\":\"4-8s\",\"executionOrder\":\"5-sequential-last\"}", 
                serviceName, userId, LocalDateTime.now().format(formatter));
        
        logger.info("Completed {} call for user: {} with result: {}", serviceName, userId, result);
        return result;
    }
    
    private void simulateApiCall(int minDelayMs, int maxDelayMs) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API call simulation interrupted", e);
        }
    }
    
    /**
     * Enable or disable error simulation for testing purposes.
     * 
     * @param enabled true to enable error simulation, false to disable
     */
    public static void setErrorSimulationEnabled(boolean enabled) {
        errorSimulationEnabled = enabled;
        Logger logger = LoggerFactory.getLogger(ExternalApiActivitiesImpl.class);
        logger.info("Error simulation {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Check if error simulation is currently enabled.
     * 
     * @return true if error simulation is enabled, false otherwise
     */
    public static boolean isErrorSimulationEnabled() {
        return errorSimulationEnabled;
    }
}