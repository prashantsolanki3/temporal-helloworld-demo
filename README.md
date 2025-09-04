# Temporal Hello World Spring Boot Demo

This is a simple Hello World demonstration of integrating Temporal with Spring Boot. It shows how to create workflows, activities, and REST endpoints to trigger Temporal workflows.

## Project Structure

```
temporal-helloworld-demo/
├── pom.xml
├── docker-compose.yml
├── dynamicconfig/
│   └── development-sql.yaml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── temporal/
│       │           └── demos/
│       │               └── helloworld/
│       │                   ├── HelloWorldApplication.java
│       │                   ├── config/
│       │                   │   └── TemporalConfig.java
│       │                   ├── activities/
│       │                   │   ├── HelloWorldActivities.java
│       │                   │   └── HelloWorldActivitiesImpl.java
│       │                   ├── controllers/
│       │                   │   └── HelloWorldController.java
│       │                   └── workflows/
│       │                       ├── HelloWorldWorkflow.java
│       │                       └── HelloWorldWorkflowImpl.java
│       └── resources/
│           └── application.yml
└── README.md
```

## Features

- **Latest Dependencies**: Updated to Spring Boot 3.3.3, Java 21, and Temporal SDK 1.25.2
- **Temporal Activities**: Two simple activities that create greeting messages
- **Temporal Workflow**: A workflow that orchestrates the activities
- **REST Endpoints**: GET and POST endpoints to trigger workflows
- **Docker Compose**: Simple POC-level Temporal server setup with PostgreSQL
- **Manual Configuration**: Uses direct Temporal SDK configuration (no Spring Boot starter dependencies)

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.9+**
3. **Docker** and **Docker Compose**

## Quick Start

### 1. Start Temporal Server

Start the complete Temporal stack using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database (port 5432)
- Temporal server (port 7233)
- Temporal Web UI (port 8080)
- Admin tools container

Wait a few seconds for all services to be ready.

### 2. Verify Temporal is Running

Check the Temporal Web UI: `http://localhost:8080`

### 3. Build and Run the Application

```bash
# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

The application will start on port `8090`.

## Testing the Endpoints

### GET Endpoint

Test with default name "World":
```bash
curl http://localhost:8090/api/hello
```

Test with custom name:
```bash
curl "http://localhost:8090/api/hello?name=Alice"
```

### POST Endpoint

Test with JSON payload:
```bash
curl -X POST \
  http://localhost:8090/api/hello \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob"}'
```

## Expected Response

The workflow will execute two activities and return a combined greeting:
```
Hello, Alice! Welcome to Temporal, Alice!
```

## Monitoring

Monitor workflow execution in the Temporal Web UI:
1. Open `http://localhost:8080` in your browser
2. Look for workflows in the `default` namespace
3. Search for workflow IDs starting with `hello-world-`

## Architecture

### Latest Technologies Used

- **Spring Boot 3.3.3**: Latest Spring Boot with native compilation support
- **Java 21**: Latest LTS Java version
- **Temporal SDK 1.25.2**: Latest stable Temporal Java SDK
- **PostgreSQL 15**: Modern PostgreSQL database
- **Temporal Server 1.24.2**: Latest Temporal server

### Components

#### Activities (`HelloWorldActivities`)
- `sayHello(String name)`: Returns a simple greeting
- `createGreeting(String greeting, String name)`: Creates a custom greeting

#### Workflow (`HelloWorldWorkflow`)
- `executeHelloWorld(String name)`: Orchestrates the activities and returns the combined result

#### Controller (`HelloWorldController`)
- `GET /api/hello`: Triggers workflow with query parameter
- `POST /api/hello`: Triggers workflow with JSON body

#### Configuration (`TemporalConfig`)
- Manual Spring configuration for Temporal client and workers
- No dependency on alpha Spring Boot starter

## Configuration

The application uses:
- **Application port**: 8090
- **Temporal server**: localhost:7233 (via Docker Compose)
- **Task queue**: HelloWorldTaskQueue
- **Database**: PostgreSQL (via Docker Compose)

## Docker Compose Services

- **postgresql**: Database backend for Temporal
- **temporal**: Main Temporal server
- **temporal-web**: Web UI for monitoring
- **temporal-admin-tools**: CLI tools for administration

## Cleanup

To stop all services:

```bash
docker-compose down
```

To remove all data:

```bash
docker-compose down -v
```

## Troubleshooting

1. **Connection refused**: Ensure Docker Compose services are running with `docker-compose ps`
2. **Port conflicts**: Check if ports 5432, 7233, 8080, or 8090 are already in use
3. **Java version**: Ensure you're using Java 21 or higher
4. **Docker issues**: Try `docker-compose down && docker-compose up -d` to restart

## Next Steps

This POC can be extended with:
- Production-ready database configuration
- SSL/TLS security
- Workflow versioning
- Custom metrics and monitoring
- Distributed deployment
- CI/CD pipeline integration