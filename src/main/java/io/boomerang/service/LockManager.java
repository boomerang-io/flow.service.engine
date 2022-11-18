package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.TaskRunEntity;

public interface LockManager {
  public void acquireLock(TaskRunEntity taskExecution, String activityId);
  public void releaseLock(TaskRunEntity taskExecution, String activityId);
  public String acquireWorkflowLock(List<String> keys);
  void releaseWorkflowLock(List<String> keys, String tokenId);
}
