# Temporal Hello World Demo

A comprehensive Spring Boot application demonstrating core Temporal concepts for distributed system orchestration.

## 🎯 Overview

This demo showcases how to build resilient, scalable workflows using Temporal with Spring Boot. It demonstrates the fundamental building blocks of Temporal's durable execution platform through a simple "Hello World" example.

## 🔧 Temporal Concepts Demonstrated

### 1. **Workflow Interface & Implementation**
```java
@WorkflowInterface
public interface HelloWorldWorkflow {
    @WorkflowMethod
    String executeHelloWorld(String name);
}
```
- **Purpose**: Defines the contract for workflow orchestration
- **Key Features**: Type-safe workflow definitions with `@WorkflowInterface` and `@WorkflowMethod`

### 2. **Activity Interface & Implementation**
```java
@ActivityInterface
public interface HelloWorldActivities {
    @ActivityMethod
    String sayHello(String name);
    
    @ActivityMethod
    String createGreeting(String greeting, String name);
}
```
- **Purpose**: Encapsulates actual business logic and external service calls
- **Key Features**: Isolated, testable units of work with `@ActivityInterface` and `@ActivityMethod`

### 3. **Workflow-Activity Orchestration Pattern**
```java
public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {
    private final HelloWorldActivities activities = Workflow.newActivityStub(
        HelloWorldActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    );
    
    @Override
    public String executeHelloWorld(String name) {
        // Orchestrate multiple activities
        String greeting = activities.sayHello(name);
        String finalGreeting = activities.createGreeting("Welcome to Temporal", name);
        return greeting + " " + finalGreeting;
    }
}
```
- **Purpose**: Coordinates multiple activities while maintaining state
- **Key Features**: Durable execution, automatic retries, state persistence

### 4. **Spring Boot Integration**
```java
@Configuration
public class TemporalConfig {
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }
    
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }
    
    @Bean
    public Worker worker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());
        workerFactory.start();
        return worker;
    }
}
```
- **Purpose**: Seamless integration with Spring's dependency injection
- **Key Features**: Bean configuration, lifecycle management, dependency injection

### 5. **Task Queue Architecture**
- **Task Queue**: `"HelloWorldTaskQueue"`
- **Purpose**: Routes work to appropriate workers
- **Benefits**: Load balancing, scaling, fault isolation

### 6. **Workflow Execution & Client Interaction**
```java
@RestController
public class HelloWorldController {
    @GetMapping("/api/hello")
    public String sayHello(@RequestParam(defaultValue = "World") String name) {
        HelloWorldWorkflow workflow = workflowClient.newWorkflowStub(
            HelloWorldWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("hello-world-" + UUID.randomUUID())
                .setTaskQueue(TASK_QUEUE)
                .build()
        );
        return workflow.executeHelloWorld(name);
    }
}
```
- **Purpose**: Trigger workflows via REST API
- **Key Features**: Unique workflow IDs, type-safe execution

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Temporal Server (local or cloud)

### Running Temporal Server Locally
```bash
# Option 1: Using Docker Compose (included in project)
./start-temporal.sh

# Option 2: Using Temporal CLI
temporal server start-dev
```

### Running the Application
```bash
# Start the Spring Boot application
mvn spring-boot:run

# Application will be available at http://localhost:8090
```

## 📡 API Endpoints

### GET /api/hello
**Description**: Execute Hello World workflow with default or custom name

**Examples**:
```bash
# Default greeting
curl http://localhost:8090/api/hello
# Response: "Hello, World! Welcome to Temporal, World!"

# Custom name
curl "http://localhost:8090/api/hello?name=Alice"
# Response: "Hello, Alice! Welcome to Temporal, Alice!"
```

### POST /api/hello
**Description**: Execute Hello World workflow with JSON payload

**Example**:
```bash
curl -X POST http://localhost:8090/api/hello \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob"}'
# Response: "Hello, Bob! Welcome to Temporal, Bob!"
```

## 🔍 Key Benefits Demonstrated

### 1. **Durability**
- Workflow state is automatically persisted
- Survives process restarts and failures
- Complete execution history maintained

### 2. **Reliability** 
- Built-in retry mechanisms for activities
- Configurable timeout policies
- Automatic error handling and recovery

### 3. **Scalability**
- Workers can be scaled independently
- Horizontal scaling across multiple instances
- Load balancing via task queues

### 4. **Visibility**
- Complete workflow execution history
- Real-time monitoring and debugging
- Temporal Web UI integration

### 5. **Testability**
- Activities can be unit tested independently
- Workflow logic can be tested in isolation
- Time-travel debugging capabilities

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST Client   │───▶│ Spring Boot App │───▶│ Temporal Worker │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ Workflow Client │───▶│ Temporal Server │
                       └─────────────────┘    └─────────────────┘
                                                      │
                                                      ▼
                                              ┌─────────────────┐
                                              │   Persistence   │
                                              │   (Database)    │
                                              └─────────────────┘
```

## 🛠️ Development Patterns

### Activity Design Best Practices
- Keep activities idempotent
- Use appropriate timeouts
- Handle errors gracefully
- Keep activities focused and single-purpose

### Workflow Design Best Practices
- Use workflows for orchestration only
- Avoid blocking operations in workflows
- Use activities for external service calls
- Design for long-running processes

## 🧪 Testing

### Unit Testing Activities
```java
@Test
public void testSayHello() {
    HelloWorldActivitiesImpl activities = new HelloWorldActivitiesImpl();
    String result = activities.sayHello("Test");
    assertEquals("Hello, Test!", result);
}
```

### Integration Testing Workflows
```java
@Test
public void testHelloWorldWorkflow() {
    TestWorkflowRule testWorkflowRule = TestWorkflowRule.newBuilder()
        .setWorkflowTypes(HelloWorldWorkflowImpl.class)
        .setActivityImplementations(new HelloWorldActivitiesImpl())
        .build();
    
    HelloWorldWorkflow workflow = testWorkflowRule.getWorkflowClient()
        .newWorkflowStub(HelloWorldWorkflow.class);
    
    String result = workflow.executeHelloWorld("Test");
    assertTrue(result.contains("Hello, Test!"));
}
```

## 📚 Learning Resources

- [Temporal Documentation](https://docs.temporal.io/)
- [Java SDK Guide](https://docs.temporal.io/docs/java/introduction)
- [Spring Boot Integration](https://docs.temporal.io/docs/java/spring-boot-integration)
- [Temporal Samples](https://github.com/temporalio/samples-java)

## 🎯 Next Steps

To extend this demo, consider exploring:

1. **Advanced Patterns**
   - Saga patterns for distributed transactions
   - Child workflows for complex orchestration
   - Signals and queries for external communication

2. **Production Features**
   - Workflow versioning strategies
   - Monitoring and alerting setup
   - Security and authentication

3. **Integration Examples**
   - Database transactions
   - External API calls
   - File processing workflows
   - Event-driven architectures

This demo provides a solid foundation for building production-ready distributed systems with Temporal's durable execution platform.