package com.temporal.demos.helloworld.controllers;

import com.temporal.demos.helloworld.config.TemporalConfig;
import com.temporal.demos.helloworld.workflows.HelloWorldWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class HelloWorldController {
    
    @Autowired
    private WorkflowClient workflowClient;
    
    @GetMapping("/hello")
    public String sayHello(@RequestParam(defaultValue = "World") String name) {
        // Create a workflow stub
        HelloWorldWorkflow workflow = workflowClient.newWorkflowStub(
                HelloWorldWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("hello-world-" + UUID.randomUUID())
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build()
        );
        
        // Execute the workflow
        return workflow.executeHelloWorld(name);
    }
    
    @PostMapping("/hello")
    public String sayHelloPost(@RequestBody HelloRequest request) {
        // Create a workflow stub
        HelloWorldWorkflow workflow = workflowClient.newWorkflowStub(
                HelloWorldWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("hello-world-" + UUID.randomUUID())
                        .setTaskQueue(TemporalConfig.TASK_QUEUE)
                        .build()
        );
        
        // Execute the workflow
        return workflow.executeHelloWorld(request.getName());
    }
    
    public static class HelloRequest {
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}