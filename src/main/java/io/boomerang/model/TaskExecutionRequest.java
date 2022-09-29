package io.boomerang.model;

import java.util.Map;
import io.boomerang.model.enums.TaskType;

public class TaskExecutionRequest {
  
  private String workflowRunId;
  
  private String taskRunId;
  
  private Map<String, String> inputs;
  
  private Map<String, String> resources;
  
  private TaskType taskType;
  
  private boolean preApproved;

  public String getWorkflowRunId() {
    return workflowRunId;
  }

  public void setWorkflowRunId(String workflowRunId) {
    this.workflowRunId = workflowRunId;
  }

  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  public Map<String, String> getResources() {
    return resources;
  }

  public void setResources(Map<String, String> resources) {
    this.resources = resources;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public boolean isPreApproved() {
    return preApproved;
  }

  public void setPreApproved(boolean preApproved) {
    this.preApproved = preApproved;
  }

  public String getTaskRunId() {
    return taskRunId;
  }

  public void setTaskRunId(String taskRunId) {
    this.taskRunId = taskRunId;
  }
}
