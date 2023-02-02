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

  ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> start(String workflowRunId, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> end(String workflowRunId);

  Page<WorkflowRunEntity> query(Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> ids);

  ResponseEntity<WorkflowRun> cancel(String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String workflowRunId, boolean start);
  
}
