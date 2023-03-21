package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.TaskRun;
import io.boomerang.model.TaskRunEndRequest;
import io.boomerang.model.TaskRunStartRequest;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.util.ParameterUtil;

/*
 * Handles CRUD of TaskRuns
 */
@Service
public class TaskRunServiceImpl implements TaskRunService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Lazy
  @Autowired
  private TaskExecutionClient taskExecutionClient;

  @Lazy
  @Autowired
  private TaskExecutionService taskExecutionService;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public ResponseEntity<TaskRun> get(String taskRunId) {
    if (taskRunId == null || taskRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
    Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
    if (optTaskRunEntity.isPresent()) {
      TaskRun taskRun = new TaskRun(optTaskRunEntity.get());
      return ResponseEntity.ok(taskRun);
    } else {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
  }

  @Override
  //TODO: change this to return TaskRuns
  public Page<TaskRunEntity> query(Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase) {
    List<Criteria> criteriaList = new ArrayList<>();

    //TODO: centralize the checks in a common filter class
    if (queryLabels.isPresent()) {
      queryLabels.get().stream().forEach(l -> {
        String decodedLabel = "";
        try {
          decodedLabel = URLDecoder.decode(l, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        LOGGER.debug(decodedLabel.toString());
        String[] label = decodedLabel.split("[=]+");
        Criteria labelsCriteria =
            Criteria.where("labels." + label[0].replace(".", "#")).is(label[1]);
        criteriaList.add(labelsCriteria);
      });
    }

    if (queryStatus.isPresent()) {
      if (queryStatus.get().stream().allMatch(q -> EnumUtils.isValidEnumIgnoreCase(RunStatus.class, q))) {
        Criteria criteria = Criteria.where("status").in(queryStatus.get());
        criteriaList.add(criteria);
      } else {
        throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "status");
      }
    }

    if (queryPhase.isPresent()) {
      if (queryPhase.get().stream().allMatch(q -> EnumUtils.isValidEnumIgnoreCase(RunPhase.class, q))) {
        Criteria criteria = Criteria.where("phase").in(queryPhase.get());
        criteriaList.add(criteria);
      } else {
        throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "phase");
      }
    }
    
    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    query.with(pageable);

    Page<TaskRunEntity> pages = PageableExecutionUtils.getPage(
        mongoTemplate.find(query.with(pageable), TaskRunEntity.class), pageable,
        () -> mongoTemplate.count(query, TaskRunEntity.class));
    
    return pages;
  }

  @Override
  public ResponseEntity<TaskRun> start(String taskRunId, Optional<TaskRunStartRequest> optRunRequest) {
    if (taskRunId == null || taskRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
    Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
    if (optTaskRunEntity.isPresent()) {
      TaskRunEntity taskRunEntity = optTaskRunEntity.get();
      // Add values from Run Request
      if (optRunRequest.isPresent()) {
        taskRunEntity.putLabels(optRunRequest.get().getLabels());
        taskRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
        taskRunEntity.setParams(ParameterUtil.addUniqueParams(taskRunEntity.getParams(), optRunRequest.get().getParams()));
        if (!Objects.isNull(optRunRequest.get().getTimeout()) && optRunRequest.get().getTimeout() != 0) {
          taskRunEntity.setTimeout(optRunRequest.get().getTimeout());
        }
//        taskRunRepository.save(taskRunEntity);
      }
      taskExecutionClient.start(taskExecutionService, taskRunEntity);      
      TaskRun taskRun = new TaskRun(taskRunEntity);
      return ResponseEntity.ok(taskRun);
    } else {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<TaskRun> end(String taskRunId, Optional<TaskRunEndRequest> optRunRequest) {
    if (taskRunId == null || taskRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
    Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
    if (optTaskRunEntity.isPresent()) {
      TaskRunEntity taskRunEntity = optTaskRunEntity.get();
      // Add values from Run Request
      if (optRunRequest.isPresent()) {
        taskRunEntity.putLabels(optRunRequest.get().getLabels());
        taskRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
        if (optRunRequest.get().getError() != null) {
          taskRunEntity.setError(optRunRequest.get().getError());
        }
        if (optRunRequest.get().getStatusMessage() != null && !optRunRequest.get().getStatusMessage().isEmpty()) {
          taskRunEntity.setStatusMessage(optRunRequest.get().getStatusMessage());
        }
        taskRunEntity.setResults(optRunRequest.get().getResults());
        if (optRunRequest.get().getStatus() == null) {
          taskRunEntity.setStatus(RunStatus.succeeded);
        } else if (!(RunStatus.failed.equals(optRunRequest.get().getStatus()) || RunStatus.succeeded.equals(optRunRequest.get().getStatus()) || RunStatus.invalid.equals(optRunRequest.get().getStatus()))) {
          throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_END_STATUS);
        } else {
          taskRunEntity.setStatus(optRunRequest.get().getStatus());          
        }
      }
      taskExecutionClient.end(taskExecutionService, taskRunEntity);
      TaskRun taskRun = new TaskRun(taskRunEntity);
      return ResponseEntity.ok(taskRun);
    } else {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<TaskRun> cancel(String taskRunId) {
    if (taskRunId == null || taskRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
    Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
    if (optTaskRunEntity.isPresent()) {
      TaskRunEntity taskRunEntity = optTaskRunEntity.get();
      taskRunEntity.setStatus(RunStatus.cancelled);
      taskExecutionClient.end(taskExecutionService, taskRunEntity);
      TaskRun taskRun = new TaskRun(optTaskRunEntity.get());
      return ResponseEntity.ok(taskRun);
    } else {
      throw new BoomerangException(BoomerangError.TASK_RUN_INVALID_REF);
    }
  }
}
