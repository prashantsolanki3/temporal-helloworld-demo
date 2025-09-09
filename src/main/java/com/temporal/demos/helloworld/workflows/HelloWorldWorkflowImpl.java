package com.temporal.demos.helloworld.workflows;

import com.temporal.demos.helloworld.activities.HelloWorldActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {

    private final HelloWorldActivities activities = Workflow.newActivityStub(
            HelloWorldActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());

    @Override
    public String executeHelloWorld(String name) {
        // First activity: Say hello
        String greeting = activities.sayHello(name);

        // Second activity: Create a more elaborate greeting
        String finalGreeting = activities.createGreeting("Welcome to Temporal", name);

        // Return combined result
        return greeting + " " + finalGreeting;
    }
}