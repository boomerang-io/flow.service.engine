package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;

public interface WorkflowRunService {

  ResponseEntity<?> get(String workflowRunId);

  Page<WorkflowRunEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<?> submit(Optional<WorkflowExecutionRequest> executionRequest);

  ResponseEntity<?> start(Optional<WorkflowExecutionRequest> executionRequest);

  ResponseEntity<?> end(Optional<WorkflowExecutionRequest> executionRequest);
  
}
