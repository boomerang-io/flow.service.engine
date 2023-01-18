package io.boomerang.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TaskRunStartRequest {

  private Map<String, String> labels = new HashMap<>();

  private Map<String, Object> annotations = new HashMap<>();
  
  private List<RunParam> params = new LinkedList<>();
  
  private Map<String, String> workspaces;
  
  private boolean preApproved;

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

  public List<RunParam> getParams() {
    return params;
  }

  public void setParams(List<RunParam> params) {
    this.params = params;
  }

  public Map<String, String> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(Map<String, String> resources) {
    this.workspaces = resources;
  }

  public boolean isPreApproved() {
    return preApproved;
  }

  public void setPreApproved(boolean preApproved) {
    this.preApproved = preApproved;
  }
}
