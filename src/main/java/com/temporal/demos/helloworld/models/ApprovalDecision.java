package com.temporal.demos.helloworld.models;

// import lombok.Getter;

// @Getter
public class ApprovalDecision {

    private String approverEmail;
    private String comments;
    private String reason;

    public ApprovalDecision(String approverEmail, String comments, String reason) {
        this.approverEmail = approverEmail;
        this.comments = comments;
        this.reason = reason;
    }

    public String getApproverEmail() {
        return approverEmail;
    }

    public String getComments() {
        return comments;
    }

    public String getReason() {
        return reason;
    }
}