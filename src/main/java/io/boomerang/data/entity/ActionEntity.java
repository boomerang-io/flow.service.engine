package io.boomerang.data.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.Actioner;
import io.boomerang.model.enums.ActionStatus;
import io.boomerang.model.enums.ActionType;


/*
 * Entity for Manual Action and Approval Action
 * 
 * Shared with the Workflow Service
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('actions')}")
public class ActionEntity {

  @Id
  private String id;
  private String workflowRef;
  private String workflowRunRef;
  private String taskRunRef;
  private List<Actioner> actioners;
  private ActionStatus status;
  private ActionType type;
  private String instructions;
  private Date creationDate;
  private int numberOfApprovers;
  private String approverGroupRef;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getWorkflowRef() {
    return workflowRef;
  }
  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }
  public String getWorkflowRunRef() {
    return workflowRunRef;
  }
  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }
  public String getTaskRunRef() {
    return taskRunRef;
  }
  public void setTaskRunRef(String taskRunRef) {
    this.taskRunRef = taskRunRef;
  }
  public List<Actioner> getActioners() {
    return actioners;
  }
  public void setActioners(List<Actioner> actioners) {
    this.actioners = actioners;
  }
  public ActionStatus getStatus() {
    return status;
  }
  public void setStatus(ActionStatus status) {
    this.status = status;
  }
  public ActionType getType() {
    return type;
  }
  public void setType(ActionType type) {
    this.type = type;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public int getNumberOfApprovers() {
    return numberOfApprovers;
  }
  public void setNumberOfApprovers(int numberOfApprovers) {
    this.numberOfApprovers = numberOfApprovers;
  }
  public String getApproverGroupRef() {
    return approverGroupRef;
  }
  public void setApproverGroupRef(String approverGroupRef) {
    this.approverGroupRef = approverGroupRef;
  }
  public String getInstructions() {
    return instructions;
  }
  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }
}
