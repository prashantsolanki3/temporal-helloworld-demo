# Port Configuration Summary

## Current Setup:
- **Spring Boot Application**: Port 8090 (configured in application.yml)
- **Temporal Web UI**: Port 8080 (configured in docker-compose.yml)
- **Test Scripts**: Updated to use port 8090 for API calls

## Updated Files:
- ✅ `test-retry-functionality.sh` - Uses port 8090
- ✅ `RETRY_TESTING_GUIDE.md` - All examples use port 8090
- ✅ `application.yml` - Application configured for port 8090

## Quick Test Commands (Port 8090):
```bash
# Check error simulation status
curl http://localhost:8090/api/orchestration/error-simulation/status

# Enable error simulation
curl -X POST http://localhost:8090/api/orchestration/error-simulation/enable

# Test orchestration with retries
curl -X POST -H "Content-Type: application/json" \
  -d '{"userId":"test-user"}' \
  http://localhost:8090/api/orchestration/execute-sync
```