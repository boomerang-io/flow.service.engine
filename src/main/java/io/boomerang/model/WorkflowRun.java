package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.WorkflowRunEntity;

public class WorkflowRun extends WorkflowRunEntity {

  private String description;

  private String workflowName;

  private List<TaskExecutionResponse> tasks;
  
  private long workflowRevisionName;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public List<TaskExecutionResponse> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskExecutionResponse> taskRuns) {
    this.tasks = taskRuns;
  }

  public long getWorkflowRevisionName() {
    return workflowRevisionName;
  }

  public void setWorkflowRevisionName(long workflowRevisionName) {
    this.workflowRevisionName = workflowRevisionName;
  }
}
