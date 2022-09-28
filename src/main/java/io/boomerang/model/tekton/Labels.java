package io.boomerang.model.tekton;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Labels {

  private Map<String, String> unknownFields = new HashMap<>();

  @JsonAnyGetter
  public Map<String, String> otherFields() {
    return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, String value) {
    unknownFields.put(name, value);
  }
}
