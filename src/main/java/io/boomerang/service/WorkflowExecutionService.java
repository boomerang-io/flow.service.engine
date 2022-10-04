package io.boomerang.service;

import java.util.concurrent.CompletableFuture;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowExecutionService {
  CompletableFuture<Boolean> executeWorkflowVersion(WorkflowEntity workflow,
      WorkflowRevisionEntity revision, WorkflowRunEntity wfRunEntity);
}
