package com.temporal.demos.helloworld.activities;

import com.temporal.demos.helloworld.utils.RandomErrorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExternalApiActivitiesImpl implements ExternalApiActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalApiActivitiesImpl.class);
    
    // Flag to enable/disable random error simulation
    private static boolean errorSimulationEnabled = false;
    
    // In-memory payment tracking for async payments
    private static final Map<String, PaymentStatus> paymentTracker = new ConcurrentHashMap<>();
    
    private enum PaymentState {
        INITIATED, PROCESSING, COMPLETED, FAILED
    }
    
    private static class PaymentStatus {
        PaymentState state;
        @SuppressWarnings("unused")
        String userId;
        @SuppressWarnings("unused")
        double amount;
        int pollCount;
        
        PaymentStatus(String userId, double amount) {
            this.userId = userId;
            this.amount = amount;
            this.state = PaymentState.INITIATED;
            this.pollCount = 0;
        }
    }
    
    @Override
    public String callUserService(String userId) {
        logger.info("Starting UserService call for user: {}", userId);
        
        // Simple error simulation
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(20, "UserService");
        }
        
        // Simulate API call with random delay
        simulateApiCall(2000, 4000);
        
        String result = String.format("{\"service\":\"UserService\",\"userId\":\"%s\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"}", userId);
        
        logger.info("Completed UserService call for user: {}", userId);
        return result;
    }
    
    @Override
    public String callOrderService(String userId) {
        logger.info("Starting OrderService call for user: {}", userId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(25, "OrderService");
        }
        
        simulateApiCall(1000, 3000);
        
        String result = String.format("{\"service\":\"OrderService\",\"userId\":\"%s\",\"totalOrders\":5,\"totalAmount\":1250.50}", userId);
        
        logger.info("Completed OrderService call for user: {}", userId);
        return result;
    }
    
    @Override
    public String callPaymentService(String userId) {
        logger.info("Starting PaymentService call for user: {}", userId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(30, "PaymentService");
        }
        
        simulateApiCall(3000, 6000);
        
        String result = String.format("{\"service\":\"PaymentService\",\"userId\":\"%s\",\"defaultMethod\":\"**** 1234\",\"creditScore\":750}", userId);
        
        logger.info("Completed PaymentService call for user: {}", userId);
        return result;
    }
    
    @Override
    public String callNotificationService(String userId) {
        logger.info("Starting NotificationService call for user: {}", userId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(15, "NotificationService");
        }
        
        simulateApiCall(500, 2000);
        
        String result = String.format("{\"service\":\"NotificationService\",\"userId\":\"%s\",\"unreadCount\":3}", userId);
        
        logger.info("Completed NotificationService call for user: {}", userId);
        return result;
    }
    
    @Override
    public String callRecommendationService(String userId) {
        logger.info("Starting RecommendationService call for user: {}", userId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(25, "RecommendationService");
        }
        
        simulateApiCall(4000, 8000);
        
        String result = String.format("{\"service\":\"RecommendationService\",\"userId\":\"%s\",\"recommendations\":[\"Product A\",\"Product B\",\"Product C\"]}", userId);
        
        logger.info("Completed RecommendationService call for user: {}", userId);
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
    
    public static void setErrorSimulationEnabled(boolean enabled) {
        errorSimulationEnabled = enabled;
        Logger logger = LoggerFactory.getLogger(ExternalApiActivitiesImpl.class);
        logger.info("Error simulation {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    public static boolean isErrorSimulationEnabled() {
        return errorSimulationEnabled;
    }
    
    // Payment Service Implementation (Sync and Async versions)
    
    /**
     * Synchronous payment processing - completes immediately
     */
    public String processSyncPayment(String userId, double amount) {
        logger.info("Processing synchronous payment for user: {}, amount: ${}", userId, amount);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(20, "SyncPaymentService");
        }
        
        simulateApiCall(2000, 5000);
        
        String transactionId = "txn-" + UUID.randomUUID().toString().substring(0, 8);
        String result = String.format(
                "{\"service\":\"SyncPaymentService\",\"userId\":\"%s\",\"amount\":%.2f,\"status\":\"COMPLETED\",\"transactionId\":\"%s\"}",
                userId, amount, transactionId);
        
        logger.info("Synchronous payment completed for user: {}, transactionId: {}", userId, transactionId);
        return result;
    }
    
    @Override
    public String initiateAsyncPaymentProcess(String userId, double amount) {
        String paymentId = "payment-" + UUID.randomUUID().toString();
        
        logger.info("Initiating async payment process for user: {}, amount: ${}, paymentId: {}", 
                userId, amount, paymentId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(15, "AsyncPaymentService");
        }
        
        simulateApiCall(1000, 3000);
        
        // Create payment tracking entry
        PaymentStatus paymentStatus = new PaymentStatus(userId, amount);
        paymentTracker.put(paymentId, paymentStatus);
        
        // Simulate random payment processing time (2-6 poll cycles)
        int pollsToComplete = ThreadLocalRandom.current().nextInt(2, 7);
        paymentStatus.pollCount = -pollsToComplete;
        
        String result = String.format(
                "{\"service\":\"AsyncPaymentService\",\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,\"status\":\"INITIATED\"}",
                paymentId, userId, amount);
        
        logger.info("Payment process initiated successfully. PaymentId: {}", paymentId);
        return result;
    }
    
    @Override
    public String pollPaymentStatus(String paymentId) {
        logger.info("Polling payment status for paymentId: {}", paymentId);
        
        if (errorSimulationEnabled) {
            RandomErrorGenerator.maybeThrowError(10, "AsyncPaymentService-Poll");
        }
        
        simulateApiCall(500, 2000);
        
        PaymentStatus payment = paymentTracker.get(paymentId);
        if (payment == null) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }
        
        payment.pollCount++;
        
        logger.info("Payment {} poll #{}: Current state: {}", paymentId, payment.pollCount, payment.state);
        
        // Still processing - trigger retry
        if (payment.pollCount <= 0) {
            payment.state = PaymentState.PROCESSING;
            throw new RuntimeException("Payment " + paymentId + " is still processing. Poll #" + payment.pollCount);
        }
        
        // Payment processing complete
        if (errorSimulationEnabled && RandomErrorGenerator.shouldThrowError(20)) {
            payment.state = PaymentState.FAILED;
            paymentTracker.remove(paymentId);
            
            String result = String.format(
                    "{\"service\":\"AsyncPaymentService-Poll\",\"paymentId\":\"%s\",\"status\":\"FAILED\",\"totalPolls\":%d}",
                    paymentId, payment.pollCount);
            
            logger.error("Payment {} failed after {} polls", paymentId, payment.pollCount);
            return result;
        } else {
            payment.state = PaymentState.COMPLETED;
            paymentTracker.remove(paymentId);
            
            String result = String.format(
                    "{\"service\":\"AsyncPaymentService-Poll\",\"paymentId\":\"%s\",\"status\":\"COMPLETED\",\"totalPolls\":%d,\"transactionId\":\"txn-%s\"}",
                    paymentId, payment.pollCount, UUID.randomUUID().toString().substring(0, 8));
            
            logger.info("Payment {} completed successfully after {} polls", paymentId, payment.pollCount);
            return result;
        }
    }
}