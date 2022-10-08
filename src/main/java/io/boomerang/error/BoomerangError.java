package io.boomerang.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** Add reusable error list here. Map to messages.properties*/
  TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
  QUERY_INVALID_FILTERS(1001, "QUERY_INVALID_FILTERS", HttpStatus.BAD_REQUEST);
  
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
