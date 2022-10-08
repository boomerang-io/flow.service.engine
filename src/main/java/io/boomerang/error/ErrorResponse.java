package io.boomerang.error;

import org.springframework.http.HttpStatus;

public class ErrorResponse {
  
  private int code;
  private String reason;
  private String message;
  private HttpStatus status;
  
  public int getCode() {
    return code;
  }
  public void setCode(int code) {
    this.code = code;
  }
  public String getReason() {
    return reason;
  }
  public void setReason(String reason) {
    this.reason = reason;
  }
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }
  public HttpStatus getStatus() {
    return status;
  }
  public void setStatus(HttpStatus status) {
    this.status = status;
  }
}
