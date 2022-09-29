package io.boomerang.service;

import io.boomerang.data.model.TaskExecution;

public interface LockManager {
  public void acquireLock(TaskExecution taskExecution, String activityId);
  public void releaseLock(TaskExecution taskExecution, String activityId);
}
