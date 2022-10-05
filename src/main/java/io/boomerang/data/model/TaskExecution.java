package io.boomerang.data.model;

import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import io.boomerang.model.RunResult;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

public class TaskExecution {
  
  private String id = new ObjectId().toString();
  private TaskType type;
  private String name;
  private RunStatus status;
  private String runRef;
  private List<RunResult> runResults;
  private List<TaskDependency> dependencies;
  private Map<String, Object> params;
  private String decisionValue;
  private String templateRef;
  private String templateVersion;
  private TaskTemplateRevision templateRevision;
  private String workflowRef;
  private String workflowName;
  private String workflowRunRef;
  
  @Override
  public String toString() {
    return "TaskExecution [id=" + id + ", type=" + type + ", name=" + name + ", status=" + status
        + ", runRef=" + runRef + ", runResults=" + runResults + ", dependencies=" + dependencies
        + ", params=" + params + ", decisionValue="
        + decisionValue + ", templateRef=" + templateRef + ", templateVersion=" + templateVersion
        + ", templateRevision=" + templateRevision + ", workflowRef=" + workflowRef
        + ", workflowName=" + workflowName + ", workflowRunRef=" + workflowRunRef + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public String getRunRef() {
    return runRef;
  }

  public void setRunRef(String runRef) {
    this.runRef = runRef;
  }

  public List<RunResult> getRunResults() {
    return runResults;
  }

  public void setRunResults(List<RunResult> runResults) {
    this.runResults = runResults;
  }

  public List<TaskDependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<TaskDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

  public String getTemplateRef() {
    return templateRef;
  }

  public void setTemplateRef(String templateRef) {
    this.templateRef = templateRef;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(String templateVersion) {
    this.templateVersion = templateVersion;
  }

  public TaskTemplateRevision getTemplateRevision() {
    return templateRevision;
  }

  public void setTemplateRevision(TaskTemplateRevision templateRevision) {
    this.templateRevision = templateRevision;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowRunRef() {
    return workflowRunRef;
  }

  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }
}
