package io.boomerang.model.enums;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RunStatus {
  succeeded("succeeded"), completed("completed"), failed("failed"), running("running"), notstarted( // NOSONAR
      "notstarted"), invalid("invalid"), skipped("skipped"), waiting("waiting"), queued("queued"), cancelled("cancelled"); // NOSONAR

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
