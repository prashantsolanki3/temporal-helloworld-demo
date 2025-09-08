package com.temporal.demos.helloworld.models;

// import lombok.Getter;

// @Getter
public class ApprovalRequest {
    private String requestId;
    private String requestDetails;
    private String requesterEmail;


    public ApprovalRequest(String requestId, String requestDetails, String requesterEmail) {
        this.requestId = requestId;
        this.requestDetails = requestDetails;
        this.requesterEmail = requesterEmail;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestDetails() {
        return requestDetails;
    }
    public String getRequesterEmail() {
        return requesterEmail;
    }

}
