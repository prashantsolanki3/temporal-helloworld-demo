#!/bin/bash

echo "🚀 Starting Temporal Hello World Demo"
echo "======================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Start Temporal services
echo "📦 Starting Temporal services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check if Temporal is healthy
echo "🔍 Checking Temporal health..."
for i in {1..30}; do
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo "✅ Temporal Web UI is ready at http://localhost:8080"
        break
    fi
    echo "   Waiting for Temporal Web UI... ($i/30)"
    sleep 2
done

echo ""
echo "🎯 Services Status:"
echo "   - Temporal Server: localhost:7233"
echo "   - Temporal Web UI: http://localhost:8080"
echo "   - PostgreSQL: localhost:5432"
echo ""
echo "📝 Next steps:"
echo "   1. Run the Spring Boot app: mvn spring-boot:run"
echo "   2. Test the endpoint: curl 'http://localhost:8090/api/hello?name=World'"
echo ""
echo "🛑 To stop services: docker-compose down"