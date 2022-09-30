package io.boomerang.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.boomerang.data.model.TaskExecution;

/*
 * This service performs Async magic
 */
@Service
public class TaskExecutionClientImpl implements TaskExecutionClient {
  @Override
  @Async
  public void createTask(TaskExecutionService taskService, TaskExecution taskRequest) {
    taskService.createTask(taskRequest);
  }
  
  @Override
  @Async
  public void startTask(TaskExecutionService taskService, TaskExecution taskRequest) {
    taskService.startTask(taskRequest);
  }

  @Override
  @Async
  public void endTask(TaskExecutionService taskService, TaskExecution taskResponse) {
    taskService.endTask(taskResponse);
  }
}
