package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.AbstractKeyValue;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.WorkflowExecutionRequest;

public interface WorkflowRunService {

  WorkflowRunEntity createRun(WorkflowRevisionEntity entity, WorkflowExecutionRequest request,
      List<AbstractKeyValue> labels);

  List<TaskExecutionResponse> getTaskExecutions(String workflowRunId);
  
}
