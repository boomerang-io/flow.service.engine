package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;

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

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public ResponseEntity<?> get(String taskRunId) {
    Optional<TaskRunEntity> taskRunEntity = taskRunRepository.findById(taskRunId);
    if (taskRunEntity.isPresent()) {
      return ResponseEntity.ok(taskRunEntity.get());
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  //TODO: change this to return TaskRuns
  public Page<TaskRunEntity> query(Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase) {
    List<Criteria> criteriaList = new ArrayList<>();

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
      Criteria statusCriteria = Criteria.where("status").in(queryStatus.get());
      criteriaList.add(statusCriteria);
    }

    if (queryPhase.isPresent()) {
      Criteria statusCriteria = Criteria.where("phase").in(queryPhase.get());
      criteriaList.add(statusCriteria);
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
  public ResponseEntity<?> start(Optional<TaskExecutionRequest> taskExecutionRequest) {
    Optional<TaskRunEntity> taskRunEntity =
        taskRunRepository.findById(taskExecutionRequest.get().getTaskRunId());
    if (taskRunEntity.isPresent()) {
      // TODO handle updating the TaskRun with values from the request
      taskExecutionClient.startTask(taskExecutionService, taskRunEntity.get());
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
      // TODO: check if status is already completed or cancelled
      taskExecutionClient.endTask(taskExecutionService, taskRunEntity.get());
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
