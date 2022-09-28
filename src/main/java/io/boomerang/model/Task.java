package io.boomerang.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.data.dag.Dependency;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
  
  private String id;
  private TaskType type;
  private String name;
  private String templateRef;
  private List<AbstractKeyValue> params;
  private List<Dependency> dependencies;
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
  public String getTemplateRef() {
    return templateRef;
  }
  public void setTemplateRef(String templateRef) {
    this.templateRef = templateRef;
  }
  public List<AbstractKeyValue> getParams() {
    return params;
  }
  public void setParams(List<AbstractKeyValue> params) {
    this.params = params;
  }
  public List<Dependency> getDependencies() {
    return dependencies;
  }
  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }
}
