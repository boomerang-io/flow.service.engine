package io.boomerang.entity.model.dag;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dependency {

  private String taskId;

  private String switchCondition;

  private ExecutionCondition executionCondition;

  private boolean conditionalExecution;

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getSwitchCondition() {
    return switchCondition;
  }

  public void setSwitchCondition(String switchCondition) {
    this.switchCondition = switchCondition;
  }

  public ExecutionCondition getExecutionCondition() {
    return executionCondition;
  }

  public void setExecutionCondition(ExecutionCondition executionCondition) {
    this.executionCondition = executionCondition;
  }

  public boolean isConditionalExecution() {
    return conditionalExecution;
  }

  public void setConditionalExecution(boolean conditionalExecution) {
    this.conditionalExecution = conditionalExecution;
  }
}
