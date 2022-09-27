package io.boomerang.model.params;

import io.boomerang.model.ParamType;

public class AbstractParams {
  private String name;
  private ParamType type;
  private String description;
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ParamType getType() {
    return type;
  }

  public void setType(ParamType type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
