package io.boomerang.service;

import io.boomerang.data.entity.TaskRunEntity;

public interface TaskExecutionClient {
  
  public void queue(TaskExecutionService taskService, TaskRunEntity taskRunEntity);  
  public void start(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
  public void end(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
}
