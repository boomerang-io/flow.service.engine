package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
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
    if (!Objects.isNull(taskRunId) && !taskRunId.isBlank()) {
      Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
      if (optTaskRunEntity.isPresent()) {
        TaskRun taskRun = new TaskRun(optTaskRunEntity.get());
        return ResponseEntity.ok(taskRun);
      }
    }
    throw new BoomerangException(BoomerangError.TASKRUN_INVALID_REF);
  }

  @Override
  public Page<TaskRun> query(Optional<Date> from, Optional<Date> to, Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort = Sort.by(new Order(querySort.orElse(Direction.ASC), "creationDate"));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }
    List<Criteria> criteriaList = new ArrayList<>();
    
    if (from.isPresent() && !to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get());
      criteriaList.add(criteria);
    } else if (!from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").lt(to.get());
      criteriaList.add(criteria);
    } else if (from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get()).lt(to.get());
      criteriaList.add(criteria);
    }

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
    if (queryLimit.isPresent()) {
      query.with(pageable);
    } else {
      query.with(sort);
    }
    
    List<TaskRunEntity> taskRunEntities = mongoTemplate.find(query, TaskRunEntity.class);
    
    List<TaskRun> taskRuns = new LinkedList<>();
    taskRunEntities.forEach(e -> taskRuns.add(new TaskRun(e)));

    Page<TaskRun> pages = PageableExecutionUtils.getPage(
        taskRuns, pageable,
        () -> taskRuns.size());
    
    return pages;
  }

  @Override
  public ResponseEntity<TaskRun> start(String taskRunId,
      Optional<TaskRunStartRequest> optRunRequest) {
    if (!Objects.isNull(taskRunId) && !taskRunId.isBlank()) {
      Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
      if (optTaskRunEntity.isPresent()) {
        TaskRunEntity taskRunEntity = optTaskRunEntity.get();
        // Add values from Run Request
        if (optRunRequest.isPresent()) {
          taskRunEntity.putLabels(optRunRequest.get().getLabels());
          taskRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
          taskRunEntity.setParams(ParameterUtil.addUniqueParams(taskRunEntity.getParams(),
              optRunRequest.get().getParams()));
          if (!Objects.isNull(optRunRequest.get().getTimeout())
              && optRunRequest.get().getTimeout() != 0) {
            taskRunEntity.setTimeout(optRunRequest.get().getTimeout());
          }
        }
        taskExecutionClient.start(taskExecutionService, taskRunEntity);
        TaskRun taskRun = new TaskRun(taskRunEntity);
        return ResponseEntity.ok(taskRun);
      }
    }
    throw new BoomerangException(BoomerangError.TASKRUN_INVALID_REF);
  }

  @Override
  public ResponseEntity<TaskRun> end(String taskRunId, Optional<TaskRunEndRequest> optRunRequest) {
    if (!Objects.isNull(taskRunId) && !taskRunId.isBlank()) {
      Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
      if (optTaskRunEntity.isPresent()) {
        TaskRunEntity taskRunEntity = optTaskRunEntity.get();
        // Add values from Run Request
        if (optRunRequest.isPresent()) {
          taskRunEntity.putLabels(optRunRequest.get().getLabels());
          taskRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
          if (optRunRequest.get().getStatusMessage() != null
              && !optRunRequest.get().getStatusMessage().isEmpty()) {
            taskRunEntity.setStatusMessage(optRunRequest.get().getStatusMessage());
          }
          taskRunEntity.setResults(optRunRequest.get().getResults());
          if (optRunRequest.get().getStatus() == null) {
            taskRunEntity.setStatus(RunStatus.succeeded);
          } else if (!(RunStatus.failed.equals(optRunRequest.get().getStatus())
              || RunStatus.succeeded.equals(optRunRequest.get().getStatus())
              || RunStatus.invalid.equals(optRunRequest.get().getStatus()))) {
            throw new BoomerangException(BoomerangError.TASKRUN_INVALID_END_STATUS);
          } else {
            taskRunEntity.setStatus(optRunRequest.get().getStatus());
          }
        }
        taskExecutionClient.end(taskExecutionService, taskRunEntity);
        TaskRun taskRun = new TaskRun(taskRunEntity);
        return ResponseEntity.ok(taskRun);
      }
    }
    throw new BoomerangException(BoomerangError.TASKRUN_INVALID_REF);
  }

  @Override
  public ResponseEntity<TaskRun> cancel(String taskRunId) {
    if (!Objects.isNull(taskRunId) && !taskRunId.isBlank()) {
      Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskRunId);
      if (optTaskRunEntity.isPresent()) {
        TaskRunEntity taskRunEntity = optTaskRunEntity.get();
        taskRunEntity.setStatus(RunStatus.cancelled);
        taskExecutionClient.end(taskExecutionService, taskRunEntity);
        TaskRun taskRun = new TaskRun(optTaskRunEntity.get());
        return ResponseEntity.ok(taskRun);
      }
    }
    throw new BoomerangException(BoomerangError.TASKRUN_INVALID_REF);
  }
}
