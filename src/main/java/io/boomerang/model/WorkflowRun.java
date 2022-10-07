package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.data.entity.WorkflowRunEntity;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "duration", "workflowName", "workflowRef", "workflowRevisionRef", "labels", "params", "tasks" })
public class WorkflowRun extends WorkflowRunEntity {

  private String description;

  private String workflowName;

  private List<TaskRun> tasks;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this, "labels");
    this.putLabels(entity.getLabels());
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

  public List<TaskRun> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskRun> taskRuns) {
    this.tasks = taskRuns;
  }
}
