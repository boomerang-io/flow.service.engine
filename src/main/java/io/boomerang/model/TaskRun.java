package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.entity.TaskRunEntity;

public class TaskRun extends TaskRunEntity {
  
  private String name;
  
  private String workflowId;
  
  private String workflowName;
  
  public TaskRun() {
    
  }

  public TaskRun(TaskRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
}
