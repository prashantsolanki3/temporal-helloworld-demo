package com.temporal.demos.helloworld.activities;

import org.springframework.stereotype.Component;

@Component
public class HelloWorldActivitiesImpl implements HelloWorldActivities {
    
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
    
    @Override
    public String createGreeting(String greeting, String name) {
        return greeting + ", " + name + "!";
    }
}