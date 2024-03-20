package io.boomerang.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** 
   * Add reusable error list here. Map to messages.properties
   * 
   * @param code the internal codes documented for end users and systems to integrate
   * @param reason Key to look up in messages.properties
   * @param HttpStatus status to respond as
   * 
   * Last updated 2023/10/05 - keep this date updated as this is used across the services.
   * 
   * 0-999: Matches existing ranges of public HTTP status codes
   * 10xx: System & Generic errors such as invalid queries or request based issues
   * 11xx: Team based errors
   * 12xx: Workflow based errors
   * 13xx: WorkflowRun based errors
   * 14xx: Task based errors
   * 15xx: TaskRun based errors
   * 16xx: Action based errors
   * 17xx: Schedule based errors
   * 18xx: Param based errors
   * 
   * */
  USER_UNABLE_TO_DELETE(101, "USER_UNABLE_TO_DELETE", HttpStatus.BAD_REQUEST),
  IMPORT_WORKFLOW_FAILED(400, "IMPORT_WORKFLOW_FAILED", HttpStatus.BAD_REQUEST),
  PERMISSION_DENIED(403, "PERMISSION_DENIED", HttpStatus.UNAUTHORIZED),
  WORKFLOW_TEAM_INACTIVE(423, "WORKFLOW_TEAM_INACTIVE", HttpStatus.LOCKED),
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  WORKFLOW_TRIGGER_DISABLED(429, "WORKFLOW_TRIGGER_DISABLED", HttpStatus.TOO_MANY_REQUESTS),
  DATA_CONVERSION_FAILED(500, "DATA_CONVERSION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR),
  QUERY_INVALID_FILTERS(1001, "QUERY_INVALID_FILTERS", HttpStatus.BAD_REQUEST),
  UNABLE_PATCH_REL(1002, "UNABLE_PATCH_REL", HttpStatus.INTERNAL_SERVER_ERROR),
  TOKEN_INVALID_REQ(1010, "TOKEN_INVALID_REQ", HttpStatus.BAD_REQUEST),
  TOKEN_INVALID_SESSION_REQ(1011, "TOKEN_INVALID_SESSION_REQ", HttpStatus.BAD_REQUEST),
  TOKEN_INVALID_PERMISSION(1012, "TOKEN_INVALID_PERMISSION", HttpStatus.BAD_REQUEST),
  QUOTA_EXCEEDED(1020, "QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS),
  TEAM_INVALID_REF(1101, "TEAM_INVALID_REF", HttpStatus.BAD_REQUEST),
  TEAM_NON_UNIQUE_NAME(1102, "TEAM_NON_UNIQUE_NAME", HttpStatus.UNPROCESSABLE_ENTITY),
  TEAM_INVALID_USER_ROLE(1103, "TEAM_INVALID_USER_ROLE", HttpStatus.UNPROCESSABLE_ENTITY),
  WORKFLOW_INVALID_REF(1201, "WORKFLOW_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOW_NOT_ACTIVE(1202, "WORKFLOW_NOT_ACTIVE", HttpStatus.BAD_REQUEST),
  WORKFLOW_DELETED(1203, "WORKFLOW_DELETED", HttpStatus.BAD_REQUEST),
  WORKFLOW_REVISION_NOT_FOUND(1204, "WORKFLOW_REVISION_NOT_FOUND", HttpStatus.CONFLICT),
  WORKFLOW_NON_UNIQUE_TASK_NAME(1205, "WORKFLOW_NON_UNIQUE_TASK_NAME", HttpStatus.BAD_REQUEST),
  WORKFLOW_INVALID_TASK_REF(1206, "WORKFLOW_INVALID_TASK_REF", HttpStatus.BAD_REQUEST),
  WORKFLOWRUN_INVALID_REF(1301, "WORKFLOWRUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  WORKFLOWRUN_INVALID_REQ(1302, "WORKFLOWRUN_INVALID_REQUIREMENT", HttpStatus.BAD_REQUEST),
  WORKFLOWRUN_INVALID_DEPENDENCY(1303, "WORKFLOWRUN_INVALID_DEPENDENCY", HttpStatus.BAD_REQUEST),
  WORKFLOWRUN_INVALID_PHASE(1304, "WORKFLOWRUN_INVALID_PHASE", HttpStatus.BAD_REQUEST),
  WORKFLOWRUN_TRIGGER_DISABLED(1305, "WORKFLOWRUN_TRIGGER_DISABLED", HttpStatus.BAD_REQUEST),
  TASK_INVALID_REF(1401, "TASK_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASK_ALREADY_EXISTS(1402, "TASK_ALREADY_EXISTS", HttpStatus.BAD_REQUEST),
  TASK_INVALID_NAME(1403, "TASK_INVALID_NAME", HttpStatus.BAD_REQUEST),
  TASK_INACTIVE_STATUS(1404, "TASK_INACTIVE_STATUS", HttpStatus.BAD_REQUEST),
  TASKRUN_INVALID_REF(1501, "TASKRUN_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  TASKRUN_INVALID_REQ(1502, "TASKRUN_INVALID_REQUIREMENT", HttpStatus.BAD_REQUEST),
  TASKRUN_INVALID_PARAMS(1503, "TASKRUN_INVALID_PARAMS", HttpStatus.BAD_REQUEST),
  TASKRUN_INVALID_TYPE(1504, "TASKRUN_INVALID_TYPE", HttpStatus.BAD_REQUEST),
  TASKRUN_INVALID_END_STATUS(1505, "TASKRUN_INVALID_END_STATUS", HttpStatus.BAD_REQUEST),
  ACTION_INVALID_REF(1601, "ACTION_INVALID_REF", HttpStatus.BAD_REQUEST),
  SCHEDULE_INVALID_REF(1701, "SCHEDULE_INVALID_REF", HttpStatus.BAD_REQUEST),
  SCHEDULE_INVALID_REQ(1702, "SCHEDULE_INVALID_REQ", HttpStatus.BAD_REQUEST),
  PARAMS_INVALID_REFERENCE(1801, "PARAMS_INVALID_REFERENCE", HttpStatus.BAD_REQUEST),
  PARAMS_NON_UNIQUE_KEY(1802, "PARAMS_NON_UNIQUE_KEY", HttpStatus.BAD_REQUEST);
  
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
