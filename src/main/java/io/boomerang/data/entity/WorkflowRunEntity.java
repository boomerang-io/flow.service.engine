package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.RunResult;
import io.boomerang.model.WorkflowRunParam;
import io.boomerang.model.enums.RunStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_runs')}")
public class WorkflowRunEntity   {

  @Id
  private String id;

  private String initiatedByRef;
  
  private Date creationDate;

  private Map<String, String> labels = new HashMap<>();

  private long duration;

  private RunStatus status;

  private RunStatus statusOverride;

  private String statusMessage;
  
//  private ErrorResponse error;

  private String workflowRef;

  private String workflowRevisionRef;

  private String trigger;
  
  private List<WorkflowRunParam> params = new LinkedList<>();

  private List<RunResult> results = new LinkedList<>();
  
//private List<Resources> resources;

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public void putLabels(Map<String, String> labels) {
    this.labels.putAll(labels);
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInitiatedByRef() {
    return initiatedByRef;
  }

  public void setInitiatedByRef(String initiatedByRef) {
    this.initiatedByRef = initiatedByRef;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public RunStatus getStatusOverride() {
    return statusOverride;
  }

  public void setStatusOverride(RunStatus statusOverride) {
    this.statusOverride = statusOverride;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public String getWorkflowRevisionRef() {
    return workflowRevisionRef;
  }

  public void setWorkflowRevisionRef(String workflowRevisionRef) {
    this.workflowRevisionRef = workflowRevisionRef;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public List<WorkflowRunParam> getParams() {
    return params;
  }

  public void setParams(List<WorkflowRunParam> params) {
    this.params = params;
  }

  public List<RunResult> getResults() {
    return results;
  }

  public void setResults(List<RunResult> results) {
    this.results = results;
  }
  
}
