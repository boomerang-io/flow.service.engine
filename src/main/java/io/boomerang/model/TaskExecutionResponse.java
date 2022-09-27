package io.boomerang.model;

import java.util.Date;
import org.springframework.data.annotation.Id;
import io.boomerang.data.model.RunStatus;

public class TaskExecutionResponse {

  @Id
  private String id;

  private String nodeId;
  
  private String runId;

  private long duration;

  private RunStatus status;

//  private long order;
  private Date startTime;
  private String taskId;
  private String taskName;
  private String workflowId;
  private TaskType taskType;
  
  private boolean preApproved;
  private String switchValue;
  
//  private Action approval;
  
  private String workflowRunId;
  private RunStatus workflowRunStatus;
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getNodeId() {
    return nodeId;
  }
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }
  public String getRunId() {
    return runId;
  }
  public void setRunId(String runId) {
    this.runId = runId;
  }
  public long getDuration() {
    return duration;
  }
  public void setDuration(long duration) {
    this.duration = duration;
  }
  public RunStatus getStatus() {
    return status;
  }
  public void setStatus(RunStatus status) {
    this.status = status;
  }
  public Date getStartTime() {
    return startTime;
  }
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }
  public String getTaskId() {
    return taskId;
  }
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }
  public String getTaskName() {
    return taskName;
  }
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }
  public String getWorkflowId() {
    return workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
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
  public String getSwitchValue() {
    return switchValue;
  }
  public void setSwitchValue(String switchValue) {
    this.switchValue = switchValue;
  }
  public String getWorkflowRunId() {
    return workflowRunId;
  }
  public void setWorkflowRunId(String workflowRunId) {
    this.workflowRunId = workflowRunId;
  }
  public RunStatus getWorkflowRunStatus() {
    return workflowRunStatus;
  }
  public void setWorkflowRunStatus(RunStatus workflowRunStatus) {
    this.workflowRunStatus = workflowRunStatus;
  }
  
//  private ErrorResponse error;
  
  
}
