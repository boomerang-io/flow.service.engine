package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.TaskRun;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunInsight;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.WorkflowStatus;
import io.boomerang.util.ParameterUtil;

@Service
public class WorkflowRunServiceImpl implements WorkflowRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private WorkflowExecutionClient workflowExecutionClient;

  @Autowired
  private WorkflowExecutionService workflowExecutionService;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    Optional<WorkflowRunEntity> workflowRunEntity = workflowRunRepository.findById(workflowRunId);
    if (workflowRunEntity.isPresent()) {
      WorkflowRun wfRun = new WorkflowRun(workflowRunEntity.get());
      if (withTasks) {
        wfRun.setTasks(getTaskRuns(workflowRunId));
      }
      return ResponseEntity.ok(wfRun);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  public Page<WorkflowRun> query(Optional<Date> from, Optional<Date> to, Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase, Optional<List<String>> queryIds) {
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

    //TODO add the ability to OR labels not just AND
    if (queryLabels.isPresent()) {
      queryLabels.get().stream().forEach(l -> {
        String decodedLabel = "";
        try {
          decodedLabel = URLDecoder.decode(l, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new BoomerangException(e, BoomerangError.QUERY_INVALID_FILTERS, "labels");
        }
        LOGGER.debug(decodedLabel.toString());
        String[] label = decodedLabel.split("[=]+");
        Criteria labelsCriteria =
            Criteria.where("labels." + label[0].replace(".", "#")).is(label[1]);
        criteriaList.add(labelsCriteria);
      });
    }

    if (queryStatus.isPresent()) {
      if (queryStatus.get().stream()
          .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(RunStatus.class, q))) {
        Criteria criteria = Criteria.where("status").in(queryStatus.get());
        criteriaList.add(criteria);
      } else {
        throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "status");
      }
    }

    if (queryPhase.isPresent()) {
      if (queryPhase.get().stream()
          .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(RunPhase.class, q))) {
        Criteria criteria = Criteria.where("phase").in(queryPhase.get());
        criteriaList.add(criteria);
      } else {
        throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "phase");
      }
    }
    
    if (queryIds.isPresent()) {
      Criteria criteria = Criteria.where("id").in(queryIds.get());
      criteriaList.add(criteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    query.with(pageable);
    
    List<WorkflowRunEntity> wfRunEntities = mongoTemplate.find(query.with(pageable), WorkflowRunEntity.class);
    
    List<WorkflowRun> wfRuns = new LinkedList<>();
    wfRunEntities.forEach(e -> wfRuns.add(new WorkflowRun(e)));

    Page<WorkflowRun> pages = PageableExecutionUtils.getPage(
        wfRuns, pageable,
        () -> mongoTemplate.count(query, WorkflowRunEntity.class));

    return pages;
  }

  /*
   * Generates stats / insights for a given set of filters
   */
  @Override
  public ResponseEntity<WorkflowRunInsight> insights(Optional<Date> from, Optional<Date> to,
      Optional<List<String>> labels, Optional<List<String>> ids) {
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

    //TODO add the ability to OR labels not just AND
    if (labels.isPresent()) {
      labels.get().stream().forEach(l -> {
        String decodedLabel = "";
        try {
          decodedLabel = URLDecoder.decode(l, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new BoomerangException(e, BoomerangError.QUERY_INVALID_FILTERS, "labels");
        }
        LOGGER.debug(decodedLabel.toString());
        String[] label = decodedLabel.split("[=]+");
        Criteria labelsCriteria =
            Criteria.where("labels." + label[0].replace(".", "#")).is(label[1]);
        criteriaList.add(labelsCriteria);
      });
    }
    
    if (ids.isPresent()) {
      Criteria criteria = Criteria.where("id").in(ids.get());
      criteriaList.add(criteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    
    List<WorkflowRunEntity> wfRunEntities = mongoTemplate.find(query, WorkflowRunEntity.class);
    
    // Collect the Stats
    Long totalDuration = 0L;
    Long duration;

    for (WorkflowRunEntity wfRunEntity : wfRunEntities) {
      duration = wfRunEntity.getDuration();
      if (duration != null) {
        totalDuration += duration;
      }
//      addActivityDetail(executions, activity);
    }
    
    WorkflowRunInsight wfRunInsight = new WorkflowRunInsight();
    wfRunInsight.setTotalRuns(Long.valueOf(wfRunEntities.size()));
    wfRunInsight.setConcurrentRuns(wfRunEntities.stream().filter(run -> RunPhase.running.equals(run.getPhase())).count());
    wfRunInsight.setTotalDuration(totalDuration);
    if (wfRunEntities.size() != 0) {
      wfRunInsight.setMedianDuration(totalDuration / wfRunEntities.size());
    } else {
      wfRunInsight.setMedianDuration(0L);
    }
    return ResponseEntity.ok(wfRunInsight);
  }

  /*
   * Queues the Workflow to be executed (and optionally starts the execution)
   * 
   * TODO: implement Triggers && Status - triggers may only be a Workflow service concern (same as Relationship)
   */
  @Override
  public ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    final Optional<WorkflowEntity> optWorkflow = workflowRepository.findById(workflowId);
    WorkflowEntity workflow = new WorkflowEntity();
    if (optWorkflow.isPresent()) {
      workflow = optWorkflow.get();
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    
    // Ensure Workflow is active to be able to be executed
    if (!WorkflowStatus.active.equals(workflow.getStatus())) {
      throw new BoomerangException(BoomerangError.WORKFLOW_NOT_ACTIVE);
    }
    
    Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity;
    if (version.isPresent()) {
      optWorkflowRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndVersion(workflowId, version.get());
      if (!optWorkflowRevisionEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.WORKFLOW_REVISION_NOT_FOUND);
      }

      LOGGER.debug("Workflow Revision: " + optWorkflowRevisionEntity.get().toString());
    } else {
      optWorkflowRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);
    }
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity wfRevision = optWorkflowRevisionEntity.get();
      final WorkflowRunEntity wfRunEntity = new WorkflowRunEntity();
      wfRunEntity.setWorkflowRevisionRef(wfRevision.getId());
      wfRunEntity.setWorkflowRef(wfRevision.getWorkflowRef());
      wfRunEntity.setCreationDate(new Date());
      wfRunEntity.setStatus(RunStatus.notstarted);
      wfRunEntity.putLabels(workflow.getLabels());
      wfRunEntity.setParams(ParameterUtil.paramSpecToRunParam(wfRevision.getParams()));
      wfRunEntity.setWorkspaces(wfRevision.getWorkspaces());
      if (!Objects.isNull(wfRevision.getTimeout()) && wfRevision.getTimeout() != 0) {
        wfRunEntity.setTimeout(wfRevision.getTimeout());
      }
      if (!Objects.isNull(wfRevision.getRetries()) && wfRevision.getRetries() != 0) {
        wfRunEntity.setRetries(wfRevision.getRetries());
      }

      // Add values from Run Request if Present
      if (optRunRequest.isPresent()) {
        logPayload(optRunRequest.get());
        wfRunEntity.putLabels(optRunRequest.get().getLabels());
        wfRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
        wfRunEntity.setParams(ParameterUtil.addUniqueParams(wfRunEntity.getParams(), optRunRequest.get().getParams()));
        wfRunEntity.getWorkspaces().addAll(optRunRequest.get().getWorkspaces());
        if (!Objects.isNull(optRunRequest.get().getTimeout()) && optRunRequest.get().getTimeout() != 0) {
          wfRunEntity.setTimeout(optRunRequest.get().getTimeout());
        }
        if (!Objects.isNull(optRunRequest.get().getRetries()) && optRunRequest.get().getRetries() != 0) {
          wfRunEntity.setRetries(optRunRequest.get().getRetries());
        }
      }

      // Set Trigger
      if (optRunRequest.get().getTrigger().isBlank()) {
        wfRunEntity.setTrigger("api");
      } else {
        wfRunEntity.setTrigger(optRunRequest.get().getTrigger());
      }
      
      //Add System Generated Annotations
      Map<String, Object> annotations = new HashMap<>();
      annotations.put("io.boomerang/generation", "4");
      annotations.put("io.boomerang/kind", "WorkflowRun");
      if (start) {
        //Add annotation to know this was created with ?start=true
        wfRunEntity.getAnnotations().put("io.boomerang/submit-with-start", "true");
      }
      wfRunEntity.getAnnotations().putAll(annotations);

      workflowRunRepository.save(wfRunEntity);

      // TODO: Check if Workflow is active and triggers enabled
      // Throws Execution exception if not able to
      // workflowService.canExecuteWorkflow(workflowId);

      workflowExecutionClient.queue(workflowExecutionService, wfRunEntity);

      if (start) {
        return this.start(wfRunEntity.getId(), Optional.empty());
      } else {
        final WorkflowRun response = new WorkflowRun(wfRunEntity);
        return ResponseEntity.ok(response);
      }
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REQ);
    }
  }

  @Override
  public ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    final Optional<WorkflowRunEntity> optWfRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (optWfRunEntity.isPresent()) {
      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
      // Add values from Run Request
      if (optRunRequest.isPresent()) {
        logPayload(optRunRequest.get());
        wfRunEntity.putLabels(optRunRequest.get().getLabels());
        wfRunEntity.putAnnotations(optRunRequest.get().getAnnotations());
        wfRunEntity.setParams(ParameterUtil.addUniqueParams(wfRunEntity.getParams(), optRunRequest.get().getParams()));
        wfRunEntity.getWorkspaces().addAll(optRunRequest.get().getWorkspaces());
        workflowRunRepository.save(wfRunEntity);
      }
      workflowExecutionClient.start(workflowExecutionService, wfRunEntity);
      
      //Retrieve the refreshed status
      WorkflowRunEntity updatedWfRunEntity = 
          workflowRunRepository.findById(workflowRunId).get();
      final WorkflowRun response = new WorkflowRun(updatedWfRunEntity);
      return ResponseEntity.ok(response);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<WorkflowRun> finalize(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    final Optional<WorkflowRunEntity> optWfRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (optWfRunEntity.isPresent()) {
      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();

      workflowExecutionClient.end(workflowExecutionService, wfRunEntity);
      final WorkflowRun response = new WorkflowRun(wfRunEntity);
      return ResponseEntity.ok(response);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<WorkflowRun> cancel(String workflowRunId) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    final Optional<WorkflowRunEntity> optWfRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (optWfRunEntity.isPresent()) {
      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();

      workflowExecutionClient.cancel(workflowExecutionService, wfRunEntity);
      final WorkflowRun response = new WorkflowRun(wfRunEntity);
      return ResponseEntity.ok(response);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  /*
   * To be used internally within the Engine
   */
  @Override
  public ResponseEntity<WorkflowRun> timeout(String workflowRunId, boolean taskRunTimeout) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    final Optional<WorkflowRunEntity> optWfRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (optWfRunEntity.isPresent()) {
      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
      wfRunEntity.getAnnotations().put("io.boomerang/timeout-cause", taskRunTimeout ? "TaskRun" : "WorkflowRun");
      wfRunEntity.setStatus(RunStatus.timedout);
      workflowRunRepository.save(wfRunEntity);
      workflowExecutionClient.timeout(workflowExecutionService, wfRunEntity);
      final WorkflowRun response = new WorkflowRun(wfRunEntity);
      return ResponseEntity.ok(response);
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  @Override
  public ResponseEntity<WorkflowRun> retry(String workflowRunId, boolean start, long retryCount) {
    if (workflowRunId == null || workflowRunId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    final Optional<WorkflowRunEntity> optWfRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (optWfRunEntity.isPresent()) {
      WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
      wfRunEntity.setCreationDate(new Date());
      wfRunEntity.setStatus(RunStatus.notstarted);
      wfRunEntity.setPhase(RunPhase.pending);
      wfRunEntity.setId(null);
      wfRunEntity.setStatusMessage(null);
      wfRunEntity.setDuration(0);
      wfRunEntity.setStartTime(null);
      wfRunEntity.getAnnotations().put("io.boomerang/retry-count", retryCount);
      wfRunEntity.getAnnotations().remove("io.boomerang/timeout-cause");
      if (!wfRunEntity.getAnnotations().containsKey("io.boomerang/retry-of")) {
        wfRunEntity.getAnnotations().put("io.boomerang/retry-of", workflowRunId);
        wfRunEntity.getLabels().put("io.boomerang/retry-of", workflowRunId);
      }      
      workflowRunRepository.save(wfRunEntity);

      workflowExecutionClient.queue(workflowExecutionService, wfRunEntity);

      if (start) {
        return this.start(wfRunEntity.getId(), Optional.empty());
      } else {
        final WorkflowRun response = new WorkflowRun(wfRunEntity);
        return ResponseEntity.ok(response);
      }
    } else {
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
  }

  private List<TaskRun> getTaskRuns(String workflowRunId) {
    List<TaskRunEntity> taskRunEntities = taskRunRepository.findByWorkflowRunRef(workflowRunId);
    return taskRunEntities.stream().map(t -> new TaskRun(t)).collect(Collectors.toList());


    //
    // TODO: Update the following or make sure they are set on the run at execution end task time.
    // if (TaskType.approval.equals(run.getTaskType())
    // || TaskType.manual.equals(run.getTaskType())) {
    // Action approval = approvalService.getApprovalByTaskActivits(task.getId());
    // response.setApproval(approval);
    // } else if (TaskType.runworkflow == task.getTaskType()
    // && task.getRunWorkflowActivityId() != null) {
    //
    // String runWorkflowActivityId = task.getRunWorkflowActivityId();
    // ActivityEntity activity =
    // this.flowActivityService.findWorkflowActivtyById(runWorkflowActivityId);
    // if (activity != null) {
    // response.setRunWorkflowActivityStatus(activity.getStatus());
    // }
    // } else if (TaskType.eventwait == task.getTaskType()) {
    // List<TaskOutputResult> results = new LinkedList<>();
    // TaskOutputResult result = new TaskOutputResult();
    // result.setName("eventPayload");
    // result.setDescription("Payload that was received with the Wait For Event");
    // if (task.getOutputs() != null) {
    // String json = task.getOutputs().get("eventPayload");
    // result.setValue(json);
    // }
    // results.add(result);
    // response.setResults(results);
    // } else if (TaskType.template == task.getTaskType()
    // || TaskType.customtask == task.getTaskType() || TaskType.script == task.getTaskType()) {
    // List<TaskOutputResult> results = new LinkedList<>();
    // setupTaskOutputResults(task, response, results);
    //
    // }
  }

  private void logPayload(WorkflowRunRequest request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writeValueAsString(request);
      LOGGER.info("Received Request Payload: ");
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(e.getStackTrace());
    }
  }
}
