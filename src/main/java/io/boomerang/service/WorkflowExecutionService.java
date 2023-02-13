package io.boomerang.service;

import java.util.concurrent.CompletableFuture;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowExecutionService {

  void queue(WorkflowRunEntity wfRunEntity);

  CompletableFuture<Boolean> start(WorkflowRunEntity wfRunEntity);

  void end(WorkflowRunEntity wfRunEntity);

  void cancel(WorkflowRunEntity workflowExecution);

  void timeout(WorkflowRunEntity workflowExecution);
}
