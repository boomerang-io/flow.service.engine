package io.boomerang.service;

import org.springframework.retry.support.RetryTemplate;
import io.boomerang.data.entity.TaskRunEntity;

public interface LockManager {
  public String acquireTaskLock(TaskRunEntity taskExecution, String activityId);
  public void releaseTaskLock(TaskRunEntity taskExecution, String activityId);
  public String acquireRunLock(String key);
  public void releaseRunLock(String key, String tokenId);
}
