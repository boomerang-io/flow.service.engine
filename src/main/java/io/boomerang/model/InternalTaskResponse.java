package io.boomerang.model;

import java.util.HashMap;
import java.util.Map;
import io.boomerang.model.enums.RunStatus;

public class InternalTaskResponse {

  private RunStatus status;

  private String activityId;
  
  private Map<String, String> outputProperties = new HashMap<>();

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public Map<String, String> getOutputProperties() {
    return outputProperties;
  }

  public void setOutputProperties(Map<String, String> outputProperties) {
    this.outputProperties = outputProperties;
  }

}
