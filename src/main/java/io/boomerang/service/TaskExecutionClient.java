package io.boomerang.service;

import io.boomerang.data.model.TaskExecution;

public interface TaskExecutionClient {
  
  public void createTask(TaskExecutionService taskService, TaskExecution taskExecution);  
  public void startTask(TaskExecutionService taskService, TaskExecution taskRequest);
  public void endTask(TaskExecutionService taskService, TaskExecution taskResponse);
}
