package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;
import io.boomerang.util.TaskMapper;

/*
 * Handles CRUD of TaskRuns
 */
@Service
public class TaskRunServiceImpl implements TaskRunService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private TaskExecutionClient taskExecutionClient;

  @Autowired
  private TaskExecutionService taskExecutionService;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Override
  public List<TaskRun> query(Optional<String> labels) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResponseEntity<?> start(Optional<TaskExecutionRequest> taskExecutionRequest) {
    Optional<TaskRunEntity> taskRunEntity =
        taskRunRepository.findById(taskExecutionRequest.get().getTaskRunId());
    if (taskRunEntity.isPresent()) {
      TaskExecution taskExecution =
          TaskMapper.taskExecutionRequestToExecutionTask(taskExecutionRequest.get());
      taskExecutionClient.startTask(taskExecutionService, taskExecution);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<?> end(Optional<TaskExecutionRequest> taskExecutionRequest) {
    Optional<TaskRunEntity> taskRunEntity =
        taskRunRepository.findById(taskExecutionRequest.get().getTaskRunId());
    if (taskRunEntity.isPresent()) {
      TaskExecution taskExecution =
          TaskMapper.taskExecutionRequestToExecutionTask(taskExecutionRequest.get());
      taskExecutionClient.endTask(taskExecutionService, taskExecution);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

}
