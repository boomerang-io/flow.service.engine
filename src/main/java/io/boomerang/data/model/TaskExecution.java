package io.boomerang.data.model;

import java.util.List;
import java.util.Map;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

public class TaskExecution {
  
  private String id;
  private TaskType type;
  private String name;
  private RunStatus status;
  private String templateRef;
  private String templateVersion;
  private String runRef;
  private List<TaskDependency> dependencies;
  private Map<String, Object> params;
  private String workflowRef;
  private String workflowName;
  private String workflowRunRef;
  private boolean enableLifecycle;
  private String decisionValue;
  private TaskTemplateRevision revision;
  private List<TaskTemplateResult> results;
  
  @Override
  public String toString() {
    return "TaskExecution [id=" + id + ", type=" + type + ", name=" + name + ", status=" + status
        + ", templateRef=" + templateRef + ", templateVersion=" + templateVersion + ", runRef="
        + runRef + ", dependencies=" + dependencies + ", params=" + params + ", workflowRef="
        + workflowRef + ", workflowName=" + workflowName + ", workflowRunRef="
            + workflowRunRef + ", enableLifecycle=" + enableLifecycle
        + ", decisionValue=" + decisionValue + ", revision=" + revision + ", results=" + results
        + "]";
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
  public String getRunRef() {
    return runRef;
  }
  public void setRunRef(String runRef) {
    this.runRef = runRef;
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
  public boolean isEnableLifecycle() {
    return enableLifecycle;
  }
  public void setEnableLifecycle(boolean enableLifecycle) {
    this.enableLifecycle = enableLifecycle;
  }
  public String getDecisionValue() {
    return decisionValue;
  }
  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }
  public TaskTemplateRevision getRevision() {
    return revision;
  }
  public void setRevision(TaskTemplateRevision revision) {
    this.revision = revision;
  }
  public List<TaskTemplateResult> getResults() {
    return results;
  }
  public void setResults(List<TaskTemplateResult> results) {
    this.results = results;
  }
  public String getWorkflowRunRef() {
    return workflowRunRef;
  }
  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }
}
