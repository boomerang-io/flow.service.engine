package io.boomerang.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.boomerang.model.enums.ExecutionCondition;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDependency {

  private String taskRef;

  private String decisionCondition;

  private ExecutionCondition executionCondition = ExecutionCondition.always;

  public String getTaskRef() {
    return taskRef;
  }

  public void setTaskRef(String taskRef) {
    this.taskRef = taskRef;
  }

  public String getDecisionCondition() {
    return decisionCondition;
  }

  public void setDecisionCondition(String decisionCondition) {
    this.decisionCondition = decisionCondition;
  }

  public ExecutionCondition getExecutionCondition() {
    return executionCondition;
  }

  public void setExecutionCondition(ExecutionCondition executionCondition) {
    this.executionCondition = executionCondition;
  }
}
