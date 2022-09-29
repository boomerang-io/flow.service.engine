package io.boomerang.model.tekton;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Labels {

  private Map<String, String> otherFields = new HashMap<>();

  @JsonAnyGetter
  public Map<String, String> otherFields() {
    return otherFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, String value) {
    otherFields.put(name, value);
  }
}
