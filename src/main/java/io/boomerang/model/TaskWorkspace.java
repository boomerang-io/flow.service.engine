package io.boomerang.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskWorkspace {

  private String name;

  private String workspaceRef;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWorkspaceRef() {
    return workspaceRef;
  }

  public void setWorkspaceRef(String workspaceRef) {
    this.workspaceRef = workspaceRef;
  }
}
