package com.temporal.demos.helloworld.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrchestrationWorkflow {
    
    @WorkflowMethod
    String orchestrateExternalApiCalls(String userId, boolean useAsyncPayment);
}