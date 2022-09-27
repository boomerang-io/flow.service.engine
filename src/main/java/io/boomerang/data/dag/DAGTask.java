
package io.boomerang.data.dag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.data.model.TaskTemplateResult;
import io.boomerang.model.AbstractKeyValue;
import io.boomerang.model.TaskType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"taskId", "taskType", "taskName", "templateId", "templateVersion", "decisionValue", "dependencies",
    "params", "metadata", "additionalProperties"})
public class DAGTask {

  private String taskId;

  private TaskType taskType;
  
  private String taskName;
  
  private String templateId;

  private Integer templateVersion;

  private String decisionValue;

  private List<Dependency> dependencies = null;

  private Map<String, Object> metadata;
  
  private List<AbstractKeyValue> params;
  
  private List<TaskTemplateResult> results;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }
  
  public List<AbstractKeyValue> getParams() {
    return params;
  }

  public void setParams(List<AbstractKeyValue> params) {
    this.params = params;
  }

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public List<TaskTemplateResult> getResults() {
    return results;
  }

  public void setResults(List<TaskTemplateResult> results) {
    this.results = results;
  }
}
