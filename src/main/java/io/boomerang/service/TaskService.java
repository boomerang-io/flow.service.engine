package io.boomerang.service;

import java.util.List;
import java.util.Map;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.model.InternalTaskResponse;

public interface TaskService {

  void createTask(TaskExecution taskExecution);

  void endTask(InternalTaskResponse request);

  List<String> updateTaskActivityForTopic(String activityId, String topic);
  
  void submitActivity(String taskActivityId, String taskStatus, Map<String, String> outputProperties);
  
}
