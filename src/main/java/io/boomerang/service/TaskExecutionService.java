package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.model.RunResult;

public interface TaskExecutionService {

  void queue(TaskRunEntity taskExecution);

  void start(TaskRunEntity taskRequest);

  void end(TaskRunEntity request);

  List<String> updateTaskRunForTopic(String activityId, String topic);

  void submitActivity(String taskRunId, String taskStatus, List<RunResult> results);  
}
