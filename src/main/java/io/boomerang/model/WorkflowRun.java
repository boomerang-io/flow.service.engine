package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.entity.WorkflowRunEntity;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.model.WorkflowScope;

public class WorkflowRun extends WorkflowRunEntity {

  private String description;

  private String workflowName;

  private List<TaskExecutionRequest> tasks;
  
  private long workflowRevisionName;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public List<TaskExecutionRequest> getSteps() {
    return steps;
  }

  public String getUserName() {
    return userName;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setSteps(List<TaskExecutionRequest> steps) {
    this.steps = steps;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WorkflowScope getScope() {
    return scope;
  }

  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }

  public long getWorkflowRevisionVersion() {
    return workflowRevisionVersion;
  }

  public void setWorkflowRevisionVersion(long workflowRevisionVersion) {
    this.workflowRevisionVersion = workflowRevisionVersion;
  }

}
