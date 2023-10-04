package io.boomerang.service;

public interface LockManager {
  public String acquireLock(String key);
  public String acquireLock(String key, Long timeout);
  public void releaseLock(String key, String tokenId);
}
