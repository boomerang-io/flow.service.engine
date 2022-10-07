package io.boomerang.service;

import java.util.concurrent.CompletableFuture;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowExecutionService {

  void queueRevision(WorkflowRunEntity wfRunEntity);

  CompletableFuture<Boolean> startRevision(WorkflowRunEntity wfRunEntity);

  void endRevision(WorkflowRunEntity wfRunEntity);
}
