package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
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
    List<Criteria> criteriaList = new ArrayList<>();

    if (labels.isPresent()) {
      String labelsValue = labels.get();
      String decodedLabels = "";
      try {
        decodedLabels = URLDecoder.decode(labelsValue, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      String[] splitString = decodedLabels.split("[,=]+");
      
      LOGGER.info(splitString);
    }

//      List<String> keys = new ArrayList<>();
//      List<String> values = new ArrayList<>();
//
//      for (String split : splitString) {
//        if (Arrays.asList(splitString).indexOf(split) % 2 == 0) {
//          keys.add(split);
//        } else {
//          values.add(split);
//        }
//      }
//
//      for (String key : keys) {
//        Criteria labelsKeyCriteria = Criteria.where("labels.key").is(key);
//        criteriaList.add(labelsKeyCriteria);
//      }
//      for (String value : values) {
//        Criteria labelsValueCriteria = Criteria.where("labels.value").is(value);
//        criteriaList.add(labelsValueCriteria);
//      }
//    }
//    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
//    Criteria allCriteria = new Criteria();
//    if (criteriaArray.length > 0) {
//      allCriteria.andOperator(criteriaArray);
//    }
//
//    Query activityQuery = new Query(allCriteria);
//    activityQuery.with(pageable);
//
//    Page<ActivityEntity> activityPages = PageableExecutionUtils.getPage(
//        mongoTemplate.find(activityQuery.with(pageable), ActivityEntity.class), pageable,
//        () -> mongoTemplate.count(activityQuery, ActivityEntity.class));
//
//    List<FlowActivity> activityRecords =
//        filterService.convertActivityEntityToFlowActivity(activityPages.getContent());
//
//    return activityRecords;
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
