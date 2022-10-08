package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;

public interface TaskRunService {

  Page<TaskRunEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<?> start(Optional<TaskExecutionRequest> taskExecutionRequest);

  ResponseEntity<?> end(Optional<TaskExecutionRequest> taskExecutionRequest);

  ResponseEntity<?> get(String taskRunId);
}
