# Async Payment Service - Server-Side Retries Pattern

This document explains the implementation of an async payment service using Temporal's server-side retries pattern for polling, based on best practices from the Temporal community.

## Pattern Overview

Traditional polling patterns often use workflow timers and loops, but the **server-side retries pattern** leverages Temporal's built-in retry mechanism for more efficient polling.

### Traditional Approach (Not Recommended)
```java
// Workflow with polling loop
while (!paymentComplete) {
    Workflow.sleep(Duration.ofMinutes(1));
    result = activities.checkPaymentStatus(paymentId);
    if (result.isComplete()) break;
}
```

### Server-Side Retries Approach (Recommended)
```java
// Activity throws exception to trigger retry
@Override
public String pollPaymentStatus(String paymentId) {
    PaymentStatus status = checkExternalPaymentSystem(paymentId);
    
    if (status.isStillProcessing()) {
        // This triggers Temporal's retry mechanism
        throw new RuntimeException("Payment still processing");
    }
    
    return status.getResult(); // Success case
}
```

## Implementation Details

### 1. Activity Interface
```java
@ActivityInterface
public interface ExternalApiActivities {
    @ActivityMethod
    String initiateAsyncPaymentProcess(String userId, double amount);
    
    @ActivityMethod
    String pollPaymentStatus(String paymentId);
}
```

### 2. Request Structure
```java
// Request DTO with async parameter
{
    "userId": "test-user",
    "useAsyncPayment": true  // false for synchronous payment
}
```

### 2. Retry Configuration
```java
private final ActivityOptions pollingActivityOptions = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(10))  // Long timeout
    .setRetryOptions(RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofMinutes(1))   // 1 minute polls
        .setMaximumInterval(Duration.ofMinutes(1))   // Consistent interval
        .setBackoffCoefficient(1.0)                  // No backoff
        .setMaximumAttempts(20)                      // 20 minutes max
        .build())
    .build();
```

### 3. Workflow Implementation
```java
@Override
public String orchestrateExternalApiCalls(String userId, boolean useAsyncPayment) {
    if (useAsyncPayment) {
        // Step 1: Initiate async payment
        String paymentInitResult = activities.initiateAsyncPaymentProcess(userId, 150.75);
        String paymentId = extractPaymentId(paymentInitResult);
        
        // Step 2: Poll for completion (uses server-side retries)
        String paymentResult = pollingActivities.pollPaymentStatus(paymentId);
        return paymentResult;
    } else {
        // Use synchronous payment
        return activities.callPaymentService(userId);
    }
}
```

### 4. Activity Implementation
```java
@Override
public String initiateAsyncPaymentProcess(String userId, double amount) {
    String paymentId = "payment-" + UUID.randomUUID();
    
    // Start async process on external system
    simulateApiCall(1000, 3000);
    
    // Store payment tracking info
    PaymentStatus status = new PaymentStatus(userId, amount);
    paymentTracker.put(paymentId, status);
    
    return createInitiationResult(paymentId, userId, amount);
}

@Override
public String pollPaymentStatus(String paymentId) {
    PaymentStatus payment = paymentTracker.get(paymentId);
    
    // Increment poll count
    payment.pollCount++;
    
    // Check if still processing
    if (payment.pollCount <= 0) {
        // Throw exception to trigger retry
        throw new RuntimeException("Payment " + paymentId + " is still processing");
    }
    
    // Payment completed - return result
    return createCompletionResult(payment);
}
```

## Benefits of This Pattern

### ✅ Advantages
1. **Leverages Temporal's Infrastructure**: Uses built-in retry mechanism
2. **No Workflow Timers**: Avoids complex timer management in workflows
3. **Automatic Backoff**: Configurable retry intervals
4. **Worker Resilience**: Survives worker restarts/failures
5. **Clear Separation**: Business logic separate from polling logic
6. **Observability**: Retry attempts visible in Temporal UI

### ❌ Traditional Polling Issues
1. **Timer Complexity**: Managing sleep/timers in workflows
2. **Worker Dependencies**: Long-running workflows tie up workers
3. **Restart Challenges**: Complex state management on restarts
4. **Cancellation**: Difficult to cancel polling loops cleanly

## Testing the Pattern

### 1. Enable Error Simulation
```bash
curl -X POST http://localhost:8090/api/orchestration/error-simulation/enable
```

### 2. Test Async Payment with Parameter
```bash
# Test async payment using parameter
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-async-user","useAsyncPayment":true}' \
  http://localhost:8090/api/orchestration/execute-sync

# Or use the quick test endpoint
curl http://localhost:8090/api/orchestration/test-async/test-user
```

### 3. Observe Polling Behavior
Watch the application logs for:
```
INFO  - Polling payment status for paymentId: payment-12345
INFO  - Payment payment-12345 poll #1: Current state: PROCESSING
WARN  - Payment payment-12345 is still processing. Poll #1
INFO  - Payment payment-12345 poll #5: Current state: COMPLETED
INFO  - Payment payment-12345 completed successfully after 5 polls
```

## Configuration Options

### Quick Polling (Testing)
```java
.setInitialInterval(Duration.ofSeconds(10))  // 10 second polls
.setMaximumInterval(Duration.ofSeconds(10))
.setMaximumAttempts(30)                      // 5 minutes max
```

### Production Polling
```java
.setInitialInterval(Duration.ofMinutes(5))   // 5 minute polls
.setMaximumInterval(Duration.ofMinutes(5))
.setMaximumAttempts(12)                      // 1 hour max
```

### Exponential Backoff (If Desired)
```java
.setInitialInterval(Duration.ofSeconds(30))
.setMaximumInterval(Duration.ofMinutes(10))
.setBackoffCoefficient(2.0)                  // 30s, 1m, 2m, 4m, 8m, 10m...
```

## Real-World Use Cases

This pattern is ideal for:

1. **Payment Processing**: Credit card, bank transfers, crypto payments
2. **Document Processing**: PDF generation, image processing, OCR
3. **External Workflows**: Approval processes, manual reviews
4. **Data Processing**: ETL jobs, ML model training, report generation
5. **Third-Party Integrations**: Any async API that provides status endpoints

## Error Handling

The pattern handles various error scenarios:

### Transient Errors
- Network timeouts during polling
- Temporary service unavailability
- Rate limiting

### Business Errors  
- Payment failures (returned as results, not exceptions)
- Invalid payment IDs
- Insufficient funds

### System Errors
- Service permanently unavailable
- Configuration errors
- Timeout exceeded

## Monitoring and Observability

### Temporal UI
- View retry attempts in workflow execution history
- Monitor activity durations and failures
- Track polling patterns across workflows

### Application Logs
- Poll attempt counts and status
- Payment state transitions
- Error details and retry triggers

### Metrics (Recommended)
- Average polling duration
- Poll success/failure rates
- Payment completion rates
- Retry pattern effectiveness

This pattern provides a robust, scalable solution for handling async external services while leveraging Temporal's powerful retry and reliability features.