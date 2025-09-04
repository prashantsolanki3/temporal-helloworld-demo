package com.temporal.demos.helloworld.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class to generate random errors for testing Temporal's retry functionality.
 * This class provides various types of exceptions that can be thrown randomly to simulate
 * real-world failure scenarios in external API calls.
 */
public class RandomErrorGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(RandomErrorGenerator.class);
    
    // Error rates (percentages)
    public static final int DEFAULT_ERROR_RATE = 30; // 30% chance of error
    public static final int HIGH_ERROR_RATE = 50;    // 50% chance of error
    public static final int LOW_ERROR_RATE = 15;     // 15% chance of error
    
    // Different types of exceptions to simulate various failure scenarios
    @SuppressWarnings("unchecked")
    private static final Class<? extends RuntimeException>[] EXCEPTION_TYPES = new Class[]{
            RuntimeException.class,           // Generic runtime exception
            IllegalStateException.class,      // Service in invalid state
            IllegalArgumentException.class,   // Invalid input parameters
            UnsupportedOperationException.class // Operation not supported
    };
    
    private static final String[] ERROR_MESSAGES = {
            "External API temporarily unavailable",
            "Network timeout occurred",
            "Service overloaded - rate limit exceeded",
            "Database connection failed",
            "Invalid API key or authentication failed",
            "Service is under maintenance",
            "Temporary server error",
            "Request timeout - try again later",
            "Circuit breaker is open",
            "Resource not found",
            "Service dependency failure",
            "Internal server error occurred"
    };
    
    /**
     * Randomly throws an exception based on the specified error rate.
     * 
     * @param errorRate The percentage chance (0-100) of throwing an exception
     * @param serviceName The name of the service for logging purposes
     * @throws RuntimeException if the random condition is met
     */
    public static void maybeThrowError(int errorRate, String serviceName) {
        if (shouldThrowError(errorRate)) {
            RuntimeException exception = generateRandomException();
            logger.warn("Simulating random error in {}: {}", serviceName, exception.getMessage());
            throw exception;
        }
    }
    
    /**
     * Randomly throws an exception with default error rate (30%).
     * 
     * @param serviceName The name of the service for logging purposes
     * @throws RuntimeException if the random condition is met
     */
    public static void maybeThrowError(String serviceName) {
        maybeThrowError(DEFAULT_ERROR_RATE, serviceName);
    }
    
    /**
     * Determines if an error should be thrown based on the error rate.
     * 
     * @param errorRate The percentage chance (0-100) of returning true
     * @return true if an error should be thrown, false otherwise
     */
    public static boolean shouldThrowError(int errorRate) {
        if (errorRate <= 0) return false;
        if (errorRate >= 100) return true;
        
        int randomValue = ThreadLocalRandom.current().nextInt(1, 101); // 1-100
        boolean shouldThrow = randomValue <= errorRate;
        
        logger.debug("Error rate: {}%, Random value: {}, Should throw: {}", errorRate, randomValue, shouldThrow);
        return shouldThrow;
    }
    
    /**
     * Generates a random exception with a random error message.
     * 
     * @return A randomly generated RuntimeException
     */
    private static RuntimeException generateRandomException() {
        String randomMessage = getRandomErrorMessage();
        Class<? extends RuntimeException> exceptionType = getRandomExceptionType();
        
        try {
            return exceptionType.getConstructor(String.class).newInstance(randomMessage);
        } catch (Exception e) {
            // Fallback to RuntimeException if reflection fails
            logger.warn("Failed to create exception of type {}, falling back to RuntimeException", exceptionType.getSimpleName());
            return new RuntimeException(randomMessage);
        }
    }
    
    /**
     * Returns a random error message from the predefined list.
     * 
     * @return A random error message
     */
    private static String getRandomErrorMessage() {
        int index = ThreadLocalRandom.current().nextInt(ERROR_MESSAGES.length);
        return ERROR_MESSAGES[index];
    }
    
    /**
     * Returns a random exception type from the predefined list.
     * 
     * @return A random exception class
     */
    private static Class<? extends RuntimeException> getRandomExceptionType() {
        int index = ThreadLocalRandom.current().nextInt(EXCEPTION_TYPES.length);
        return EXCEPTION_TYPES[index];
    }
    
    /**
     * Simulates a transient error that might occur in external services.
     * This method introduces a random delay before potentially throwing an error.
     * 
     * @param errorRate The percentage chance of error
     * @param serviceName The service name for logging
     * @param minDelayMs Minimum delay before error check
     * @param maxDelayMs Maximum delay before error check
     */
    public static void simulateTransientError(int errorRate, String serviceName, int minDelayMs, int maxDelayMs) {
        // Add some random delay to simulate processing time before potential failure
        int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulation interrupted", e);
        }
        
        // Now check if we should throw an error
        maybeThrowError(errorRate, serviceName);
    }
    
    /**
     * Creates a custom exception with service-specific information.
     * 
     * @param serviceName The name of the failing service
     * @param userId The user ID for context
     * @param operation The operation that failed
     * @return A customized RuntimeException
     */
    public static RuntimeException createServiceException(String serviceName, String userId, String operation) {
        String message = String.format("Service '%s' failed during '%s' operation for user '%s': %s", 
                serviceName, operation, userId, getRandomErrorMessage());
        return new RuntimeException(message);
    }
}