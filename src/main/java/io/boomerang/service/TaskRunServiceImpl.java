package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;

/*
 * Handles CRUD of TaskRuns
 */
@Service
public class TaskRunServiceImpl implements TaskRunService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private TaskExecutionService taskExecutionService;

  @Override
  public List<TaskRun> query(Optional<String> labels) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskRun start(Optional<TaskExecutionRequest> taskExecutionRequest) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskRun end(Optional<TaskExecutionRequest> taskExecutionRequest) {
    // TODO Auto-generated method stub
    return null;
  }

}
