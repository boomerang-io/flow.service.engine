package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.RunResult;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_runs')}")
public class TaskRunEntity {

  @Id
  private String id;
  
  private String taskId;
  
  private TaskType taskType;
  
  private String taskName;

  private String initiatedByRef;
  
  private Date creationDate;

  private Map<String, String> labels = new HashMap<>();

  private long duration;

  private RunStatus status;

  private String statusMessage;
  
//private ErrorResponse error;
  
  private boolean preApproved;
  
  private String decisionValue;

  private String nodeId;

  private long order;
  
  private Date startTime;

  private String taskTemplateRef;

  private String taskTemplateVersion;

  private String workflowRunRef;
  
  private Map<String, Object> params = new HashMap<>();

  private List<RunResult> results;

  private List<RunResult> workflowResults;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public String getInitiatedByRef() {
    return initiatedByRef;
  }

  public void setInitiatedByRef(String initiatedByRef) {
    this.initiatedByRef = initiatedByRef;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
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

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public String getTaskTemplateRef() {
    return taskTemplateRef;
  }

  public void setTaskTemplateRef(String taskTemplateRef) {
    this.taskTemplateRef = taskTemplateRef;
  }

  public String getWorkflowRunRef() {
    return workflowRunRef;
  }

  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public List<RunResult> getResults() {
    return results;
  }

  public void setResults(List<RunResult> results) {
    this.results = results;
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

  public boolean isPreApproved() {
    return preApproved;
  }

  public void setPreApproved(boolean preApproved) {
    this.preApproved = preApproved;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public long getOrder() {
    return order;
  }

  public void setOrder(long order) {
    this.order = order;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getTaskTemplateVersion() {
    return taskTemplateVersion;
  }

  public void setTaskTemplateVersion(String taskTemplateVersion) {
    this.taskTemplateVersion = taskTemplateVersion;
  }

  public List<RunResult> getWorkflowResults() {
    return workflowResults;
  }

  public void setWorkflowResults(List<RunResult> workflowResults) {
    this.workflowResults = workflowResults;
  }
}
