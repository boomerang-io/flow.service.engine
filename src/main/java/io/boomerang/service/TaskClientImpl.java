package io.boomerang.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.model.InternalTaskResponse;

/*
 * This service performs Async magic
 */
@Service
public class TaskClientImpl implements TaskClient {
  @Override
  @Async
  public void startTask(TaskService taskService, TaskExecution taskRequest) {
    taskService.createTask(taskRequest);
  }

  @Override
  @Async
  public void endTask(TaskService taskService, InternalTaskResponse taskResponse) {
    taskService.endTask(taskResponse);
  }
}
