package io.boomerang.service;

import io.boomerang.data.entity.TaskRunEntity;

public interface LockManager {
  public void acquireLock(TaskRunEntity taskExecution, String activityId);
  public void releaseLock(TaskRunEntity taskExecution, String activityId);
}
