package io.boomerang.service;

import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowExecutionClient {

  void queue(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);

  void start(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);

  void end(WorkflowExecutionService workflowExecutionService,
      WorkflowRunEntity wfRunEntity);

  void cancel(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity);

  void timeout(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity);
}
