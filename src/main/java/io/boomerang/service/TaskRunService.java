package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.model.TaskRunRequest;

public interface TaskRunService {

  Page<TaskRunEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<?> start(String taskRunId, Optional<TaskRunRequest> taskRunRequest);

  ResponseEntity<?> end(String taskRunId);

  ResponseEntity<?> get(String taskRunId);
}
