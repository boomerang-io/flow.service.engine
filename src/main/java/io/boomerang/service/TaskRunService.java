package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;

public interface TaskRunService {

  List<TaskRun> query(Optional<String> labels);

  ResponseEntity<?> start(Optional<TaskExecutionRequest> taskExecutionRequest);

  ResponseEntity<?> end(Optional<TaskExecutionRequest> taskExecutionRequest);
}
