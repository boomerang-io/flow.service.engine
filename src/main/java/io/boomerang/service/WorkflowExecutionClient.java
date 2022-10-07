package io.boomerang.service;

import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowExecutionClient {

  void queueRevision(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);

  void startRevision(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);

  void endRevision(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);
}
