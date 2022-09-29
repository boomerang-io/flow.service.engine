package io.boomerang.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorkflowExecutionRequest {

  private String workflowRef;

  private Map<String, String> labels = new HashMap<>();
  
  boolean applyQuotas;
  
  private List<WorkflowRunParam> params = new LinkedList<>();
  
  private Map<String, String> resources;

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public boolean isApplyQuotas() {
    return applyQuotas;
  }

  public void setApplyQuotas(boolean applyQuotas) {
    this.applyQuotas = applyQuotas;
  }

  public List<WorkflowRunParam> getParams() {
    return params;
  }

  public void setParams(List<WorkflowRunParam> params) {
    this.params = params;
  }

  public Map<String, String> getResources() {
    return resources;
  }

  public void setResources(Map<String, String> resources) {
    this.resources = resources;
  }
}
