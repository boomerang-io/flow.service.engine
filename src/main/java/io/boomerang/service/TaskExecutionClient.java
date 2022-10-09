package io.boomerang.service;

import io.boomerang.data.entity.TaskRunEntity;

public interface TaskExecutionClient {
  
  public void queueTask(TaskExecutionService taskService, TaskRunEntity taskRunEntity);  
  public void startTask(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
  public void endTask(TaskExecutionService taskService, TaskRunEntity taskRunEntity);
}
