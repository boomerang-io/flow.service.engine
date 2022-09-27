package io.boomerang.data.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.dag.DAGTask;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DAG {

  private List<DAGTask> tasks;

  public List<DAGTask> getTasks() {
    return tasks;
  }

  public void setTasks(List<DAGTask> tasks) {
    this.tasks = tasks;
  }
}
