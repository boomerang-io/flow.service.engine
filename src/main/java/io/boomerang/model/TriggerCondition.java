package io.boomerang.model;

import java.util.List;
import io.boomerang.model.enums.TriggerConditionOperation;

public class TriggerCondition {
  
  private String field;
  private TriggerConditionOperation operation;
  private String value;
  private List<String> values;
  
  @Override
  public String toString() {
    return "TriggerCondition [field=" + field + ", operation=" + operation + ", value=" + value
        + ", values=" + values + ", getField()=" + getField() + ", getOperation()=" + getOperation()
        + ", getValue()=" + getValue() + ", getValues()=" + getValues() + ", getClass()="
        + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
  }
  public String getField() {
    return field;
  }
  public void setField(String field) {
    this.field = field;
  }
  public TriggerConditionOperation getOperation() {
    return operation;
  }
  public void setOperation(TriggerConditionOperation operation) {
    this.operation = operation;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
  public List<String> getValues() {
    return values;
  }
  public void setValues(List<String> values) {
    this.values = values;
  }
}
