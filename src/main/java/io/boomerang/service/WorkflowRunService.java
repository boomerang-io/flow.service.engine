package io.boomerang.service;

import java.util.List;
import java.util.Map;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.WorkflowExecutionRequest;

public interface WorkflowRunService {

  List<TaskExecutionResponse> getTaskExecutions(String workflowRunId);

  WorkflowRunEntity createRun(WorkflowRevisionEntity revision, WorkflowExecutionRequest request,
      Map<String, String> labels);
  
}
