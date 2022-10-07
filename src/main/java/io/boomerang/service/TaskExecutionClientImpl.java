package io.boomerang.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;

/*
 * Internal client allowing for async processing of individual tasks.
 * 
 * Referenced by both the external TaskRun Controller and the internal Workflow Execution.
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
