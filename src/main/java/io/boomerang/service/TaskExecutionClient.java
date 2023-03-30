package io.boomerang.service;

import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface TaskExecutionClient {
  
  public void queue(TaskExecutionService taskService, TaskRunEntity taskRunEntity);  
  public void start(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
  public void execute(TaskExecutionService taskService, TaskRunEntity taskRequest,
      WorkflowRunEntity wfRunEntity);
  public void end(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
}
