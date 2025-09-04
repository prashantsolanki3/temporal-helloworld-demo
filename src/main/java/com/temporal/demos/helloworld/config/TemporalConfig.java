package com.temporal.demos.helloworld.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.temporal.demos.helloworld.activities.HelloWorldActivitiesImpl;
import com.temporal.demos.helloworld.workflows.HelloWorldWorkflowImpl;

@Configuration
public class TemporalConfig {
    
    public static final String TASK_QUEUE = "HelloWorldTaskQueue";
    
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }
    
    @Bean
    @DependsOn("workflowServiceStubs")
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }
    
    @Bean
    @DependsOn("workflowClient")
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }
    
    @Bean
    @DependsOn("workerFactory")
    public Worker worker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());
        workerFactory.start();
        return worker;
    }
}