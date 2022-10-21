package io.boomerang.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** Add reusable error list here. Map to messages.properties*/
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  QUERY_INVALID_FILTERS(1001, "QUERY_INVALID_FILTERS", HttpStatus.BAD_REQUEST),
  WORKFLOW_INVALID_REF(1101, "WORKFLOW_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOW_RUN_INVALID_REF(1102, "WORKFLOW_RUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOW_RUN_INVALID_REQ(1103, "WORKFLOW_RUN_INVALID_REQUIREMENT", HttpStatus.BAD_REQUEST),
  WORKFLOW_REVISION_NOT_FOUND(1104, "WORKFLOW_REVISION_NOT_FOUND", HttpStatus.CONFLICT),
  TASK_RUN_INVALID_REF(1101, "TASK_RUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_RUN_INVALID_REQ(1101, "TASK_RUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_INVALID_REF(1301, "TASK_TEMPLATE_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_INVALID_VERSION(1302, "TASK_TEMPLATE_INVALID_VERSION", HttpStatus.BAD_REQUEST),
  TASK_TEMPLATE_REVISION_NOT_FOUND(1304, "TASK_TEMPLATE_REVISION_NOT_FOUND", HttpStatus.CONFLICT);
  
  private final int code;
  private final String reason;
  private final HttpStatus status;

  public int getCode() {
    return code;
  }

  public String getReason() {
    return reason;
  }

  public HttpStatus getStatus() {
    return status;
  }

  private BoomerangError(int code, String reason, HttpStatus status) {
    this.code = code;
    this.reason = reason;
    this.status = status;
  }
}
