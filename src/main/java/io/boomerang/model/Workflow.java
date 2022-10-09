package io.boomerang.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;

/*
 * Workflow Model joining Workflow Entity and Workflow Revision Entity
 * 
 * A number of the Workflow Revision elements are put under metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {
  
  private String id;

  private String name;

  private WorkflowStatus status = WorkflowStatus.active;
  
  private Integer version = 1;
  
  private String icon;

  private String description;

  private String shortDescription;
  
  private String markdown;

//  private Labels labels;
  private Map<String, String> labels = new HashMap<>();
  
//  private Annotations annotations;
  private Map<String, Object> annotations = new HashMap<>();
  
  private List<Task> tasks = new LinkedList<>();

  private List<WorkflowParam> params = new LinkedList<>();
  
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();
  
  public Workflow() {
    
  }

  public Workflow(WorkflowEntity wfEntity, WorkflowRevisionEntity wfRevisionEntity) {
    BeanUtils.copyProperties(wfEntity, this);
    BeanUtils.copyProperties(wfRevisionEntity, this, "tasks");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }
  
  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public List<Task> getTasks() {
    return tasks;
  }

  public void setTasks(List<Task> tasks) {
    this.tasks = tasks;
  }

  public List<WorkflowParam> getParams() {
    return params;
  }

  public void setParams(List<WorkflowParam> params) {
    this.params = params;
  }

  public List<WorkflowWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<WorkflowWorkspace> workspaces) {
    this.workspaces = workspaces;
  } 
}
