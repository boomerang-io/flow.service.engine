package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.TaskRunEntity;

public interface LockManager {
  public String acquireTaskLock(TaskRunEntity taskExecution, String activityId);
  public void releaseTaskLock(TaskRunEntity taskExecution, String activityId);
  public String acquireWorkflowLock(List<String> keys);
  public void releaseWorkflowLock(List<String> keys, String tokenId);
}
