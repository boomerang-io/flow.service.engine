package io.boomerang.data.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.RunStatus;
import io.boomerang.model.AbstractKeyValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_runs')}")
public class WorkflowRunEntity   {

  @Id
  private String id;

  private String initiatedById;
  
  private Date creationDate;

  private List<AbstractKeyValue> labels;

  private Long duration;

  private RunStatus status;

  private String statusMessage;
  
//  private ErrorResponse error;

  private String workflowId;

  private String workflowRevisionId;

  private String trigger;

  private List<AbstractKeyValue> inputs;

  private List<AbstractKeyValue> results;
  
//private List<Resources> resources;

  public List<AbstractKeyValue> getLabels() {
    return labels;
  }

  public void setLabels(List<AbstractKeyValue> labels) {
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

  public List<AbstractKeyValue> getInputs() {
    return inputs;
  }

  public void setInputs(List<AbstractKeyValue> inputs) {
    this.inputs = inputs;
  }

  public List<AbstractKeyValue> getResults() {
    return results;
  }

  public void setResults(List<AbstractKeyValue> results) {
    this.results = results;
  }
  
}
