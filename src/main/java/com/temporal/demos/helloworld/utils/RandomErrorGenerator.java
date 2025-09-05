package com.temporal.demos.helloworld.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple utility to generate random errors for testing Temporal's retry functionality.
 */
public class RandomErrorGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(RandomErrorGenerator.class);
    
    private static final String[] ERROR_MESSAGES = {
            "Service temporarily unavailable",
            "Network timeout occurred",
            "Database connection failed",
            "Service overloaded",
            "Temporary server error"
    };
    
    public static void maybeThrowError(int errorRate, String serviceName) {
        if (shouldThrowError(errorRate)) {
            String message = getRandomErrorMessage();
            logger.warn("Simulating error in {}: {}", serviceName, message);
            throw new RuntimeException(message);
        }
    }
    
    public static boolean shouldThrowError(int errorRate) {
        if (errorRate <= 0) return false;
        if (errorRate >= 100) return true;
        
        int randomValue = ThreadLocalRandom.current().nextInt(1, 101);
        return randomValue <= errorRate;
    }
    
    private static String getRandomErrorMessage() {
        int index = ThreadLocalRandom.current().nextInt(ERROR_MESSAGES.length);
        return ERROR_MESSAGES[index];
    }
}