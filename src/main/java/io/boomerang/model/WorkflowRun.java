package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.data.entity.WorkflowRunEntity;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "duration", "workflowRef", "workflowRevisionRef", "labels", "params", "tasks" })
public class WorkflowRun extends WorkflowRunEntity {

  private List<TaskRun> tasks;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public List<TaskRun> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskRun> taskRuns) {
    this.tasks = taskRuns;
  }
}
