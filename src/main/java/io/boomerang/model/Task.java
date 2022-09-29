package io.boomerang.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.data.model.TaskTemplateResult;
import io.boomerang.model.enums.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
  
  private String id = new ObjectId().toString();
  
  private TaskType type;
  
  private String name;
  
  private String templateRef;
  
  private Integer templateVersion;
  
  private Map<String, Object> params = new HashMap<>();
  
  private List<TaskDependency> dependencies;
  
  //This is needed as some of our Tasks allow you to define Result Definitions on the fly
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

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
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
}
