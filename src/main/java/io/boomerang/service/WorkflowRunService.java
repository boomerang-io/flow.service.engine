package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunCount;
import io.boomerang.model.WorkflowRunInsight;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.WorkflowRunSubmitRequest;

public interface WorkflowRunService {

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);

  ResponseEntity<WorkflowRun> submit(WorkflowRunSubmitRequest request, boolean start);

  ResponseEntity<WorkflowRun> start(String workflowRunId, Optional<WorkflowRunRequest> runRequest);

  ResponseEntity<WorkflowRun> finalize(String workflowRunId);

  Page<WorkflowRun> query(Optional<Date> from, Optional<Date> to,
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows);

  ResponseEntity<WorkflowRun> cancel(String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String workflowRunId, boolean start, long retryCount);

  ResponseEntity<WorkflowRun> timeout(String workflowRunId, boolean taskRunTimeout);

  ResponseEntity<WorkflowRunInsight> insights(Optional<Date> from, Optional<Date> to, Optional<List<String>> labels, Optional<List<String>> status, Optional<List<String>> workflows);

  ResponseEntity<WorkflowRunCount> count(Optional<Date> from, Optional<Date> to,
      Optional<List<String>> labels, Optional<List<String>> workflows);
  
}
