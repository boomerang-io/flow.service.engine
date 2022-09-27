package io.boomerang.data.model;

import java.util.List;
import java.util.Map;
import io.boomerang.data.dag.Dependency;
import io.boomerang.model.TaskType;

public class TaskExecution {
  
  private String id;
  private TaskType type;
  private String name;
  private RunStatus status;
  private String templateId;
  private String templateVersion;
  private String runId;
  private List<Dependency> dependencies;
  private Map<String, String> inputs;
  private String workflowId;
  private String workflowName;
  private boolean enableLifecycle;
  private String decisionValue;
  private TaskTemplateRevision revision;
  private List<TaskTemplateResult> results;
  
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
  public String getTemplateId() {
    return templateId;
  }
  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }
  public String getTemplateVersion() {
    return templateVersion;
  }
  public void setTemplateVersion(String templateVersion) {
    this.templateVersion = templateVersion;
  }
  public List<Dependency> getDependencies() {
    return dependencies;
  }
  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }
  public Map<String, String> getInputs() {
    return inputs;
  }
  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
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
  public String getRunId() {
    return runId;
  }
  public void setRunId(String runId) {
    this.runId = runId;
  }
  public RunStatus getStatus() {
    return status;
  }
  public void setStatus(RunStatus status) {
    this.status = status;
  }
}
