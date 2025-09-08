package com.temporal.demos.helloworld.models;

// import lombok.Getter;

// @Getter
public class OrchestrationRequest {

    private String userId;
    private boolean useAsyncPayment = false; // Default to synchronous payment

    public OrchestrationRequest(String userId, boolean useAsyncPayment) {
        this.userId = userId;
        this.useAsyncPayment = useAsyncPayment;
    }


    public String getUserId() {
        return userId;
    }

    public boolean isUseAsyncPayment() {
        return useAsyncPayment;
    }

}