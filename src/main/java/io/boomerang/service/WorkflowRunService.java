package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunInsight;
import io.boomerang.model.WorkflowRunRequest;

public interface WorkflowRunService {

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);

  ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> start(String workflowRunId, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> finalize(String workflowRunId);

  Page<WorkflowRun> query(Optional<Date> from, Optional<Date> to, Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> ids);

  ResponseEntity<WorkflowRun> cancel(String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String workflowRunId, boolean start, long retryCount);

  ResponseEntity<WorkflowRun> timeout(String workflowRunId, boolean taskRunTimeout);

  ResponseEntity<WorkflowRunInsight> insights(Optional<Date> from, Optional<Date> to, Optional<List<String>> labels, Optional<List<String>> status);
  
}
