package com.temporal.demos.helloworld.activities;

import com.temporal.demos.helloworld.utils.RandomErrorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExternalApiActivitiesImpl implements ExternalApiActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalApiActivitiesImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Flag to enable/disable random error simulation (can be toggled for testing)
    private static boolean errorSimulationEnabled = false;
    
    // Simulate an in-memory payment tracking system
    private static final Map<String, PaymentStatus> paymentTracker = new ConcurrentHashMap<>();
    
    // Payment status enum
    private enum PaymentState {
        INITIATED, PROCESSING, COMPLETED, FAILED
    }
    
    // Payment status data structure
    private static class PaymentStatus {
        PaymentState state;
        String userId;
        double amount;
        LocalDateTime initiatedAt;
        LocalDateTime lastUpdatedAt;
        int pollCount;
        
        PaymentStatus(String userId, double amount) {
            this.userId = userId;
            this.amount = amount;
            this.state = PaymentState.INITIATED;
            this.initiatedAt = LocalDateTime.now();
            this.lastUpdatedAt = LocalDateTime.now();
            this.pollCount = 0;
        }
    }
    
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
    
    // ========================================
    // ASYNC PAYMENT SERVICE IMPLEMENTATION
    // ========================================
    
    /**
     * Initiates an async payment process that will complete over time.
     * This simulates starting a payment process on an external system that
     * doesn't return results immediately.
     * 
     * @param userId The user ID for the payment
     * @param amount The payment amount
     * @return Payment ID that can be used to poll for status
     */
    @Override
    public String initiateAsyncPaymentProcess(String userId, double amount) {
        String serviceName = "AsyncPaymentService";
        String paymentId = "payment-" + UUID.randomUUID().toString();
        
        logger.info("Initiating async payment process for user: {}, amount: ${}, paymentId: {}", 
                userId, amount, paymentId);
        
        // Simulate potential failure during payment initiation (20% error rate)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(20, serviceName + "-Initiation");
        }
        
        // Simulate API call to external payment service (1-3 seconds)
        simulateApiCall(1000, 3000);
        
        // Create payment tracking entry
        PaymentStatus paymentStatus = new PaymentStatus(userId, amount);
        paymentTracker.put(paymentId, paymentStatus);
        
        // Simulate random payment processing time (will complete in 2-8 poll cycles)
        int pollsToComplete = ThreadLocalRandom.current().nextInt(2, 9);
        paymentStatus.pollCount = -pollsToComplete; // Negative means still processing
        
        String result = String.format(
                "{\"service\":\"%s\",\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f," +
                "\"status\":\"INITIATED\",\"timestamp\":\"%s\",\"estimatedPollsToComplete\":%d}",
                serviceName, paymentId, userId, amount, LocalDateTime.now().format(formatter), pollsToComplete);
        
        logger.info("Payment process initiated successfully. PaymentId: {}, Result: {}", paymentId, result);
        return result;
    }
    
    /**
     * Polls the status of an async payment process.
     * This method implements the server-side retries pattern by throwing an exception
     * when the payment is still processing, causing Temporal to retry automatically.
     * 
     * @param paymentId The payment ID to check
     * @return Payment status if completed, throws exception if still processing
     */
    @Override
    public String pollPaymentStatus(String paymentId) {
        String serviceName = "AsyncPaymentService-Poll";
        
        logger.info("Polling payment status for paymentId: {}", paymentId);
        
        // Simulate potential network/service failures during polling (15% error rate)
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(15, serviceName);
        }
        
        // Simulate API call to check payment status (0.5-2 seconds)
        simulateApiCall(500, 2000);
        
        PaymentStatus payment = paymentTracker.get(paymentId);
        if (payment == null) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }
        
        // Update poll count and last updated time
        payment.pollCount++;
        payment.lastUpdatedAt = LocalDateTime.now();
        
        logger.info("Payment {} poll #{}: Current state: {}", paymentId, payment.pollCount, payment.state);
        
        // Simulate payment processing progression
        if (payment.pollCount <= 0) {
            // Still processing - this will trigger a retry
            payment.state = PaymentState.PROCESSING;
            
            // Simulate occasional processing failures (10% chance)
            if (errorSimulationEnabled && RandomErrorGenerator.shouldThrowError(10)) {
                throw RandomErrorGenerator.createServiceException(serviceName, payment.userId, "PAYMENT_PROCESSING_ERROR");
            }
            
            // Throw exception to trigger retry (server-side retries pattern)
            throw new RuntimeException("Payment " + paymentId + " is still processing. Poll #" + payment.pollCount + 
                    " (Started: " + payment.initiatedAt.format(formatter) + ")");
        }
        
        // Payment processing complete - determine final status
        if (errorSimulationEnabled && RandomErrorGenerator.shouldThrowError(20)) {
            // 20% chance of payment failure
            payment.state = PaymentState.FAILED;
            String result = String.format(
                    "{\"service\":\"%s\",\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f," +
                    "\"status\":\"FAILED\",\"timestamp\":\"%s\",\"totalPolls\":%d,\"error\":\"Payment processing failed\"}",
                    serviceName, paymentId, payment.userId, payment.amount, 
                    LocalDateTime.now().format(formatter), payment.pollCount);
            
            logger.error("Payment {} failed after {} polls", paymentId, payment.pollCount);
            
            // Remove from tracker
            paymentTracker.remove(paymentId);
            
            // Return failure result (do not throw exception as this is a valid business outcome)
            return result;
        } else {
            // Payment completed successfully
            payment.state = PaymentState.COMPLETED;
            String result = String.format(
                    "{\"service\":\"%s\",\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f," +
                    "\"status\":\"COMPLETED\",\"timestamp\":\"%s\",\"totalPolls\":%d," +
                    "\"processingTime\":\"%s\",\"transactionId\":\"txn-%s\"}",
                    serviceName, paymentId, payment.userId, payment.amount, 
                    LocalDateTime.now().format(formatter), payment.pollCount,
                    java.time.Duration.between(payment.initiatedAt, payment.lastUpdatedAt).toString(),
                    UUID.randomUUID().toString().substring(0, 8));
            
            logger.info("Payment {} completed successfully after {} polls", paymentId, payment.pollCount);
            
            // Remove from tracker
            paymentTracker.remove(paymentId);
            
            return result;
        }
    }
}