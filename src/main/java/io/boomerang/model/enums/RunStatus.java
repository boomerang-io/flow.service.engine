package io.boomerang.model.enums;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RunStatus {
  completed("completed"), failure("failure"), inProgress("inProgress"), notstarted( // NOSONAR
      "notstarted"), invalid("invalid"), skipped("skipped"), waiting("waiting"), cancelled("cancelled"); // NOSONAR

  private String status;

  RunStatus(String status) {
    this.status = status;
  }

  @JsonValue
  public String getStatus() {
    return status;
  }

  public static RunStatus getRunStatus(String status) {
    return Arrays.asList(RunStatus.values()).stream()
        .filter(value -> value.getStatus().equals(status)).findFirst().orElse(null);
  }
}
