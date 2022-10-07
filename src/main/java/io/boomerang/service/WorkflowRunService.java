package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;

public interface WorkflowRunService {

  ResponseEntity<?> get(String workflowRunId);

  List<WorkflowRun> query(Optional<String> labels);

  ResponseEntity<?> submit(Optional<WorkflowExecutionRequest> executionRequest);

  ResponseEntity<?> start(Optional<WorkflowExecutionRequest> executionRequest);

  ResponseEntity<?> end(Optional<WorkflowExecutionRequest> executionRequest);
  
}
