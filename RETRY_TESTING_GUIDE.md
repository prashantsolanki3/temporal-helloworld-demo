# Temporal Retry Functionality Testing Guide

This guide explains how to test Temporal's retry functionality using the enhanced orchestration endpoints with random error simulation.

## Overview

The application has been enhanced with:
1. **RandomErrorGenerator** utility class for simulating various types of failures
2. **Enhanced ExternalApiActivitiesImpl** with configurable random errors
3. **Improved retry configuration** in OrchestrationWorkflowImpl
4. **Control endpoints** for managing error simulation
5. **Enhanced logging** to observe retry behavior

## Error Simulation

### Error Rates by Service
- **UserService**: 25% + 15% post-processing errors
- **OrderService**: 35% errors (runs in parallel)
- **PaymentService**: 40% + 20% validation errors
- **NotificationService**: 20% errors (runs in parallel)
- **RecommendationService**: 30% + 10% ML processing errors

### Retry Configuration
- **Max Attempts**: 5
- **Initial Interval**: 2 seconds
- **Max Interval**: 30 seconds
- **Backoff Coefficient**: 2.0 (exponential backoff)
- **Total Timeout**: 45 seconds per activity

## API Endpoints

### Core Orchestration Endpoints
- `POST /api/orchestration/execute` - Start orchestration asynchronously
- `POST /api/orchestration/execute-sync` - Run orchestration synchronously
- `GET /api/orchestration/status/{workflowId}` - Check workflow status
- `GET /api/orchestration/result/{workflowId}` - Get workflow result
- `GET /api/orchestration/test/{userId}` - Quick test endpoint

### Error Simulation Control
- `POST /api/orchestration/error-simulation/enable` - Enable random errors
- `POST /api/orchestration/error-simulation/disable` - Disable random errors
- `GET /api/orchestration/error-simulation/status` - Check current status

## Testing Scenarios

### Scenario 1: Basic Retry Testing
```bash
# Enable error simulation
curl -X POST http://localhost:8090/api/orchestration/error-simulation/enable

# Run synchronous orchestration
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-user-1"}' \
  http://localhost:8090/api/orchestration/execute-sync

# Check logs for retry attempts
```

### Scenario 2: Parallel Service Retries
```bash
# Enable errors and run async orchestration
curl -X POST http://localhost:8090/api/orchestration/error-simulation/enable

# Start workflow
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-user-parallel"}' \
  http://localhost:8090/api/orchestration/execute

# Monitor status (replace {workflowId} with actual ID from response)
curl http://localhost:8090/api/orchestration/status/{workflowId}
```

### Scenario 3: Comparison Testing
```bash
# Test with errors enabled
curl -X POST http://localhost:8090/api/orchestration/error-simulation/enable
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-with-errors"}' \
  http://localhost:8090/api/orchestration/execute-sync

# Test without errors
curl -X POST http://localhost:8090/api/orchestration/error-simulation/disable
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-without-errors"}' \
  http://localhost:8090/api/orchestration/execute-sync
```

## Using the Test Script

Run the provided test script for a guided demo:
```bash
./test-retry-functionality.sh
```

This script will:
1. Check current error simulation status
2. Enable error simulation
3. Run a test orchestration
4. Show retry behavior in logs
5. Optionally run a comparison without errors

## Observing Retry Behavior

### Application Logs
Watch for these log patterns:
```
INFO  - Starting UserService call for user: test-user-1
WARN  - Simulating random error in UserService: External API temporarily unavailable
INFO  - UserService completed successfully for user: test-user-1
```

### Temporal Web UI
If running Temporal Server locally with Web UI:
1. Open http://localhost:8088
2. Navigate to workflows
3. Click on your workflow execution
4. Observe the activity retry attempts in the execution history

### Response Timing
- **With errors**: Responses take longer due to retries and backoff delays
- **Without errors**: Responses complete faster with normal processing times

## Error Types Simulated

The RandomErrorGenerator creates various exception types:
- `RuntimeException` - Generic failures
- `IllegalStateException` - Service state issues
- `IllegalArgumentException` - Invalid parameters
- `UnsupportedOperationException` - Feature not available

Error messages include:
- "External API temporarily unavailable"
- "Network timeout occurred"
- "Service overloaded - rate limit exceeded"
- "Database connection failed"
- And more realistic failure scenarios

## Customization

### Adjusting Error Rates
Modify error rates in `ExternalApiActivitiesImpl`:
```java
RandomErrorGenerator.maybeThrowError(35, serviceName); // 35% error rate
```

### Changing Retry Configuration
Update `OrchestrationWorkflowImpl`:
```java
.setRetryOptions(RetryOptions.newBuilder()
    .setInitialInterval(Duration.ofSeconds(1))
    .setMaximumInterval(Duration.ofSeconds(60))
    .setBackoffCoefficient(3.0)
    .setMaximumAttempts(10)
    .build())
```

### Adding New Error Types
Extend `RandomErrorGenerator` with additional exception types or custom error scenarios.

## Production Considerations

⚠️ **Important**: Error simulation is enabled by default. For production:
1. Set `errorSimulationEnabled = false` in `ExternalApiActivitiesImpl`
2. Remove or comment out error simulation calls
3. Keep the retry configuration for handling real failures
4. Monitor actual failure rates and adjust retry policies accordingly

## Troubleshooting

### High Failure Rate
If orchestrations consistently fail:
1. Check if error simulation is enabled
2. Verify retry configuration allows enough attempts
3. Review timeout settings for activities
4. Check Temporal server connectivity

### No Retries Observed
If retries aren't happening:
1. Ensure error simulation is enabled
2. Check that exceptions are being thrown (not caught prematurely)
3. Verify retry options are properly configured
4. Confirm activity timeouts aren't too short

## Next Steps

1. **Load Testing**: Run multiple concurrent orchestrations to test retry behavior under load
2. **Circuit Breaker**: Consider implementing circuit breaker patterns for real production use
3. **Metrics**: Add metrics collection to monitor retry rates and success/failure patterns
4. **Dead Letter Queue**: Implement handling for workflows that fail after all retries