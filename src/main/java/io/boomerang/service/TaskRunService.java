package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.TaskRun;
import io.boomerang.model.TaskRunEndRequest;
import io.boomerang.model.TaskRunStartRequest;

public interface TaskRunService {

  Page<TaskRun> query(Optional<Date> from, Optional<Date> to, Optional<Integer> queryLimit,
      Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase);

  ResponseEntity<TaskRun> start(String taskRunId, Optional<TaskRunStartRequest> taskRunRequest);

  ResponseEntity<TaskRun> end(String taskRunId, Optional<TaskRunEndRequest> taskRunRequest);

  ResponseEntity<TaskRun> get(String taskRunId);
  
  ResponseEntity<TaskRun> cancel(String taskRunId);

  StreamingResponseBody streamLog(String taskRunId);
}
