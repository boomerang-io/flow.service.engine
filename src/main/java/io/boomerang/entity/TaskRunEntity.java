package io.boomerang.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.entity.model.RunStatus;
import io.boomerang.model.KeyValue;
import io.boomerang.model.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_runs')}")
public class TaskRunEntity   {

  @Id
  private String id;
  
  private TaskType taskType;

  private String initiatedById;
  
  private Date creationDate;

  private List<KeyValue> labels;

  private Long duration;

  private RunStatus status;

  private String statusMessage;
  
//  private ErrorResponse error;

  private String taskTemplateId;

  private String taskTemplateRevisionId;

  private String workflowRunId;

  private List<KeyValue> inputs;

  private List<KeyValue> results;

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

  public List<KeyValue> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValue> labels) {
    this.labels = labels;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
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

  public String getTaskTemplateRevisionId() {
    return taskTemplateRevisionId;
  }

  public void setTaskTemplateRevisionId(String taskTemplateRevisionId) {
    this.taskTemplateRevisionId = taskTemplateRevisionId;
  }

  public String getWorkflowRunId() {
    return workflowRunId;
  }

  public void setWorkflowRunId(String workflowRunId) {
    this.workflowRunId = workflowRunId;
  }

  public List<KeyValue> getInputs() {
    return inputs;
  }

  public void setInputs(List<KeyValue> inputs) {
    this.inputs = inputs;
  }

  public List<KeyValue> getResults() {
    return results;
  }

  public void setResults(List<KeyValue> results) {
    this.results = results;
  }  
}
