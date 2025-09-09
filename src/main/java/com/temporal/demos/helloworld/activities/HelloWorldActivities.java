package com.temporal.demos.helloworld.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HelloWorldActivities {

    @ActivityMethod
    String sayHello(String name);

    @ActivityMethod
    String createGreeting(String greeting, String name);
}