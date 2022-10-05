package io.boomerang.service;

import io.boomerang.data.entity.TaskRunEntity;

public interface TaskExecutionClient {
  
  public void queueTask(TaskExecutionService taskService, TaskRunEntity taskExecution);  
  public void startTask(TaskExecutionService taskService, TaskRunEntity taskRequest);
  public void endTask(TaskExecutionService taskService, TaskRunEntity taskResponse);
}
