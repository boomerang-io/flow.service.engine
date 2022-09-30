package io.boomerang.service;

import java.util.List;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.model.RunResult;

public interface TaskExecutionService {

  void createTask(TaskExecution taskExecution);

  void startTask(TaskExecution taskRequest);

  void endTask(TaskExecution request);

  List<String> updateTaskRunForTopic(String activityId, String topic);

  void submitActivity(String taskRunId, String taskStatus, List<RunResult> results);
  
}
