package io.boomerang.model;

public class WorkflowSubmitRequest extends WorkflowRunRequest {
  
  private Integer workflowVersion;
  
  private String trigger;

  public Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }
}
