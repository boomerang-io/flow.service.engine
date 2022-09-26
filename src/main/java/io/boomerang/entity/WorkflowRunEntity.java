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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_runs')}")
public class WorkflowRunEntity   {

  private List<KeyValue> labels;
  private Date creationDate;

  private Long duration;

  @Id
  private String id;

  private String initiatedById;

  private RunStatus status;

  private String statusMessage;
  
//  private ErrorResponse error;

  private String workflowId;

  private String workflowRevisionId;

  private String trigger;

  private List<KeyValue> inputs;

  private List<KeyValue> results;
  
//private List<Resources> resources;

  public List<KeyValue> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValue> labels) {
    this.labels = labels;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInitiatedById() {
    return initiatedById;
  }

  public void setInitiatedById(String initiatedById) {
    this.initiatedById = initiatedById;
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

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowRevisionId() {
    return workflowRevisionId;
  }

  public void setWorkflowRevisionid(String workflowRevisionId) {
    this.workflowRevisionId = workflowRevisionId;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
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
