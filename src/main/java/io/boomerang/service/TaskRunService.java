package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;

public interface TaskRunService {

  List<TaskRun> query(Optional<String> labels);

  TaskRun start(Optional<TaskExecutionRequest> taskExecutionRequest);

  TaskRun end(Optional<TaskExecutionRequest> taskExecutionRequest);
}
