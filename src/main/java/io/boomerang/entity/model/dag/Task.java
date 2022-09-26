
package io.boomerang.entity.model.dag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.model.TaskType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"taskId", "type", "label", "templateId", "dependencies", "properties",
    "metadata"})
public class Task {

  private String taskId;

  private TaskType type;
  
  private String label;
  
  private String templateId;

  private Integer templateVersion;

  private String decisionValue;

  private List<Dependency> dependencies = null;

  private Map<String, Object> metadata;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
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

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }
}
