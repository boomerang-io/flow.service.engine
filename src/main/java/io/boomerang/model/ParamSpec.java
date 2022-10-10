package io.boomerang.model;

import io.boomerang.model.enums.ParamType;

public class ParamSpec {
  
  private String name;
  private ParamType type;
  private String description;
  private Object defaultValue;
  
  public String getName() {
    return name;
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

  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setName(String name) {
    this.name = name;
  }
}
