package io.boomerang.service;

import java.util.concurrent.CompletableFuture;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface ExecutionService {

  CompletableFuture<Boolean> executeWorkflowVersion(WorkflowRevisionEntity revision, WorkflowRunEntity wfRunEntity);
}
