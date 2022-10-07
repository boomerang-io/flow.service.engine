package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.TaskRunEntity;

/*
 * Based on TaskRunEntity
 */
public class TaskRun extends TaskRunEntity {
  
  private String workflowName;
  
  public TaskRun() {
    
  }

  public TaskRun(TaskRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }  
}
