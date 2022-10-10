package io.boomerang.error;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @Autowired
  private MessageSource messageSource;

  @ExceptionHandler({ BoomerangException.class })
  public ResponseEntity<Object> handleBoomerangException(
      BoomerangException ex, WebRequest request) {
    
    ErrorDetail errorDetail = new ErrorDetail();
    errorDetail.setCode(ex.getCode());
    errorDetail.setDescription(ex.getDescription());

    String message = messageSource.getMessage(errorDetail.getDescription(), null, Locale.ENGLISH);
    errorDetail.setMessage(message);
    
    return new ResponseEntity<>(
        errorDetail, new HttpHeaders(), ex.getHttpStatus()); 
  }


}