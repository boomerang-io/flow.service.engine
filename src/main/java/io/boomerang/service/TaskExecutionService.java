package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface TaskExecutionService {

  void queue(TaskRunEntity taskExecution);

  void start(TaskRunEntity taskRequest);

  void execute(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity);

  void end(TaskRunEntity request);  

  List<String> updateTaskRunForTopic(String activityId, String topic);
}
