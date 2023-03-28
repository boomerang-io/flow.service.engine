package io.boomerang.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;

/*
 * Internal client allowing for async processing of individual tasks.
 * 
 * Referenced by both the external TaskRun Controller and the internal Workflow Execution.
 * 
 * Ref: https://www.baeldung.com/spring-async
 */
@Service
public class TaskExecutionClientImpl implements TaskExecutionClient {
  
  @Override
  @Async
  public void queue(TaskExecutionService taskService, TaskRunEntity taskRequest) {
    taskService.queue(taskRequest);
  }
  
  @Override
  @Async
  public void start(TaskExecutionService taskService, TaskRunEntity taskRequest) {
    taskService.start(taskRequest);
  }

  @Override
  @Async
  public void end(TaskExecutionService taskService, TaskRunEntity taskResponse) {
    taskService.end(taskResponse);
  }
}