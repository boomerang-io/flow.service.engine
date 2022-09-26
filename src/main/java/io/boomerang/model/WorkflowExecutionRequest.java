package io.boomerang.model;

import java.util.List;
import java.util.Map;

public class WorkflowExecutionRequest {

  private String workflowId;

  private List<KeyValue> labels;
  
  boolean applyQuotas;
  
  private Map<String, String> inputs;
  
  private Map<String, String> resources;

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public List<KeyValue> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValue> labels) {
    this.labels = labels;
  }

  public boolean isApplyQuotas() {
    return applyQuotas;
  }

  public void setApplyQuotas(boolean applyQuotas) {
    this.applyQuotas = applyQuotas;
  }

  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  public Map<String, String> getResources() {
    return resources;
  }

  public void setResources(Map<String, String> resources) {
    this.resources = resources;
  }
}
