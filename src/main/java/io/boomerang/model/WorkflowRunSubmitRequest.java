package io.boomerang.model;

public class WorkflowRunSubmitRequest extends WorkflowRunRequest {
  
  private String workflowRef;
  
  private Integer workflowVersion;
  
  private String trigger;
  
  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

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
