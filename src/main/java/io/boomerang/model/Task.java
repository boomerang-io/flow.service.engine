package io.boomerang.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.data.model.TaskTemplateResult;
import io.boomerang.model.enums.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
  
  private String name;
  
  private TaskType type;
  
  private String templateRef;
  
  private Integer templateVersion;
  
  private Map<String, String> labels = new HashMap<>();
  
  private Map<String, Object> annotations = new HashMap<>();
  
  private List<ParamSpec> params = new LinkedList<>();
  
  private List<TaskDependency> dependencies;
  
  //This is needed as some of our Tasks allow you to define Result Definitions on the fly
  private List<TaskTemplateResult> results;

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

  public String getTemplateRef() {
    return templateRef;
  }

  public void setTemplateRef(String templateRef) {
    this.templateRef = templateRef;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  public List<ParamSpec> getParams() {
    return params;
  }

  public void setParams(List<ParamSpec> params) {
    this.params = params;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public List<TaskDependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<TaskDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public List<TaskTemplateResult> getResults() {
    return results;
  }

  public void setResults(List<TaskTemplateResult> results) {
    this.results = results;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
