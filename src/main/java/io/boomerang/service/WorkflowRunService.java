package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.WorkflowRun;

public interface WorkflowRunService {

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);

  Page<WorkflowRunEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<WorkflowRun> submit(String workflowId, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> start(String workflowRunId, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> end(String workflowRunId);
  
}
