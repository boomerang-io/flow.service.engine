package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunCount;
import io.boomerang.model.WorkflowRunInsight;
import io.boomerang.model.WorkflowRunRequest;

public interface WorkflowRunService {

  WorkflowRun get(String workflowRunId, boolean withTasks);

  WorkflowRun start(String workflowRunId, Optional<WorkflowRunRequest> runRequest);

  WorkflowRun finalize(String workflowRunId);

  Page<WorkflowRun> query(Optional<Date> from, Optional<Date> to,
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTriggers);

  WorkflowRun cancel(String workflowRunId);

  WorkflowRun retry(String workflowRunId, boolean start, long retryCount);

  WorkflowRun timeout(String workflowRunId, boolean taskRunTimeout);

  WorkflowRunInsight insights(Optional<Date> from, Optional<Date> to, Optional<List<String>> labels, Optional<List<String>> status, Optional<List<String>> workflows);

  WorkflowRunCount count(Optional<Date> from, Optional<Date> to,
      Optional<List<String>> labels, Optional<List<String>> workflows);

  WorkflowRun run(WorkflowRunEntity request, boolean start);
}
