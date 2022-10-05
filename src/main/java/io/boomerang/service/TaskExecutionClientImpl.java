package io.boomerang.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;

/*
 * This service performs Async magic
 */
@Service
public class TaskExecutionClientImpl implements TaskExecutionClient {
  @Override
  @Async
  public void queueTask(TaskExecutionService taskService, TaskRunEntity taskRequest) {
    taskService.queueTask(taskRequest);
  }
  
  @Override
  @Async
  public void startTask(TaskExecutionService taskService, TaskRunEntity taskRequest) {
    taskService.startTask(taskRequest);
  }

  @Override
  @Async
  public void endTask(TaskExecutionService taskService, TaskRunEntity taskResponse) {
    taskService.endTask(taskResponse);
  }
}
