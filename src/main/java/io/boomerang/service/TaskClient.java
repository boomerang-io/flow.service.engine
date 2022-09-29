package io.boomerang.service;

import io.boomerang.data.model.TaskExecution;
import io.boomerang.model.InternalTaskResponse;

public interface TaskClient {
  
  public void startTask(TaskService taskService, TaskExecution taskExecution);  
  public void endTask(TaskService taskService, InternalTaskResponse taskResponse);
}
