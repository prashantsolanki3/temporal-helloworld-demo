package com.temporal.demos.helloworld.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ExternalApiActivities {

    @ActivityMethod
    String callUserService(String userId);

    @ActivityMethod
    String callOrderService(String userId);

    @ActivityMethod
    String callPaymentService(String userId);

    @ActivityMethod
    String callNotificationService(String userId);

    @ActivityMethod
    String callRecommendationService(String userId);

    // Async payment service methods using polling pattern
    @ActivityMethod
    String initiateAsyncPaymentProcess(String userId, double amount);

    @ActivityMethod
    String pollPaymentStatus(String paymentId);
}