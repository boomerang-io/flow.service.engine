package io.boomerang.data.model;

import io.boomerang.model.AbstractConfigurationProperty;

public class WorkflowProperty extends AbstractConfigurationProperty {
  
  private String jsonPath;

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }
  
}
