package io.boomerang.service;

import java.util.Optional;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowExecutionRequest;

public interface WorkflowExecutionClient {
  public WorkflowRun executeWorkflow(String workflowId,
      Optional<WorkflowExecutionRequest> executionRequest);
}
