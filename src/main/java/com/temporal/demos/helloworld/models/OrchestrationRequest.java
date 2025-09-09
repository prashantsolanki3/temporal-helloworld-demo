package com.temporal.demos.helloworld.models;

// import lombok.Getter;

// @Getter
public class OrchestrationRequest {

    private String userId;
    private String requestId;
    private boolean useAsyncPayment = false; // Default to synchronous payment

    public OrchestrationRequest(String userId, boolean useAsyncPayment, String requestId) {
        this.userId = userId;
        this.useAsyncPayment = useAsyncPayment;
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isUseAsyncPayment() {
        return useAsyncPayment;
    }

    public String getRequestId() {
        return requestId;
    }
}