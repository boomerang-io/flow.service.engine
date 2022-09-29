package io.boomerang.data.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.common.KeyValuePair;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_runs')}")
public class TaskRunEntity   {

  @Id
  private String id;
  
  private String taskId;
  
  private TaskType taskType;
  
  private String taskName;

  private String initiatedById;
  
  private Date creationDate;

  private List<KeyValuePair> labels;

  private long duration;

  private RunStatus status;

  private String statusMessage;
  
  private boolean preApproved;
  private String decisionValue;
  
//  private ErrorResponse error;

  private String nodeId;

  private long order;
  
  private Date startTime;

  private String taskTemplateId;

  private String taskTemplateVersion;

  private String workflowRunId;

  private List<KeyValuePair> inputs;

  private List<KeyValuePair> results;

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

  public String getInitiatedById() {
    return initiatedById;
  }

  public void setInitiatedById(String initiatedById) {
    this.initiatedById = initiatedById;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
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

  public String getTaskTemplateId() {
    return taskTemplateId;
  }

  public void setTaskTemplateId(String taskTemplateId) {
    this.taskTemplateId = taskTemplateId;
  }

  public String getWorkflowRunId() {
    return workflowRunId;
  }

  public void setWorkflowRunId(String workflowRunId) {
    this.workflowRunId = workflowRunId;
  }

  public List<KeyValuePair> getInputs() {
    return inputs;
  }

  public void setInputs(List<KeyValuePair> inputs) {
    this.inputs = inputs;
  }

  public List<KeyValuePair> getResults() {
    return results;
  }

  public void setResults(List<KeyValuePair> results) {
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
}
