package io.boomerang.model.tekton;

import java.util.List;
import io.boomerang.model.TaskRunResult;

public class Spec {

  private String description;
  private List<Param> params;
  private List<Step> steps;

  private List<TaskRunResult> results;
  
  public List<Step> getSteps() {
    return steps;
  }

  public void setSteps(List<Step> steps) {
    this.steps = steps;
  }

  public List<Param> getParams() {
    return params;
  }

  public void setParams(List<Param> params) {
    this.params = params;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<TaskRunResult> getResults() {
    return results;
  }

  public void setResults(List<TaskRunResult> results) {
    this.results = results;
  }
}
