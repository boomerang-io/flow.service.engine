package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
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
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.TaskRun;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.enums.RunStatus;

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
  public ResponseEntity<?> get(String workflowRunId) {
    Optional<WorkflowRunEntity> workflowRunEntity =
        workflowRunRepository.findById(workflowRunId);
    if (workflowRunEntity.isPresent()) {
      return ResponseEntity.ok(workflowRunEntity.get());
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  //TODO change this to return WorkflowRuns
  public Page<WorkflowRunEntity> query(Pageable pageable, Optional<List<String>> queryLabels,
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

    Page<WorkflowRunEntity> pages = PageableExecutionUtils.getPage(
        mongoTemplate.find(query.with(pageable), WorkflowRunEntity.class), pageable,
        () -> mongoTemplate.count(query, WorkflowRunEntity.class));
    
    return pages;
  }

  @Override
  public ResponseEntity<?> submit(Optional<WorkflowExecutionRequest> executionRequest) {
    WorkflowExecutionRequest request = new WorkflowExecutionRequest();
    if (executionRequest.isPresent()) {
      request = executionRequest.get();
      logPayload(request);
    } else {
      LOGGER.error("No Workflow Run execution request was provided.");
      return ResponseEntity.badRequest().body("No Workflow Run execution request was provided.");
    }
    final Optional<WorkflowEntity> optWorkflow =
        workflowRepository.findById(request.getWorkflowRef());
    WorkflowEntity workflow = new WorkflowEntity();
    if (optWorkflow.isPresent()) {
      workflow = optWorkflow.get();
    } else {
      LOGGER.error("No Workflow found with matching reference.");
      return ResponseEntity.badRequest().body("No Workflow found with matching reference.");
    }

    final Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity =
        this.workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflow.getId());
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity revision = optWorkflowRevisionEntity.get();
      final WorkflowRunEntity workflowRun = new WorkflowRunEntity();
      workflowRun.setWorkflowRevisionRef(revision.getId());
      workflowRun.setWorkflowRef(revision.getWorkflowRef());
      workflowRun.setCreationDate(new Date());
      workflowRun.setStatus(RunStatus.notstarted);
      workflowRun.putLabels(workflow.getLabels());
      workflowRun.putLabels(request.getLabels());
      //TODO: add default values from params on Workflow
      workflowRun.setParams(request.getParams());

      // TODO: add trigger and set initiatedBy
      // workflowRun.setTrigger(null);
      // if (!trigger.isPresent() || "manual".equals(trigger.get())) {
      // final UserEntity userEntity = userIdentityService.getCurrentUser();
      // activity.setInitiatedById(userEntity.getId());
      // }

      // TODO: add resources
      // workflowRun.setResources(null);

      final WorkflowRunEntity wfRunEntity = workflowRunRepository.save(workflowRun);
      
      // TODO: Check if Workflow is active and triggers enabled
      // Throws Execution exception if not able to
      // workflowService.canExecuteWorkflow(workflowId);

      workflowExecutionClient.queueRevision(workflowExecutionService, wfRunEntity);

//      final List<TaskExecutionResponse> taskRuns = getTaskExecutions(wfRunEntity.getId());
      final WorkflowRun response = new WorkflowRun(wfRunEntity);
//      response.setTasks(taskRuns);

      response.setTasks(getTaskRuns(wfRunEntity.getId()));
      response.setWorkflowName(workflow.getName());
      return ResponseEntity.ok(response);
    } else {
      LOGGER.error("No Workflow version found to execute.");
      return ResponseEntity.badRequest().body("No Workflow version found to execute.");
    }
  }

  @Override
  public ResponseEntity<?> start(Optional<WorkflowExecutionRequest> executionRequest) {
    final Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(executionRequest.get().getWorkflowRunRef());
    // TODO handle updating the TaskRun with values from the request
    if (wfRunEntity.isPresent()) {

        workflowExecutionClient.startRevision(workflowExecutionService, wfRunEntity.get());

//        final List<TaskExecutionResponse> taskRuns = getTaskExecutions(wfRunEntity.get().getId());
        final WorkflowRun response = new WorkflowRun(wfRunEntity.get());
        response.setTasks(getTaskRuns(wfRunEntity.get().getId()));
//        response.setTasks(taskRuns);
//        response.setWorkflowName(workflow.getName());
        return ResponseEntity.ok(response);
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  public ResponseEntity<?> end(Optional<WorkflowExecutionRequest> executionRequest) {
    Optional<WorkflowRunEntity> workflowRunEntity =
        workflowRunRepository.findById(executionRequest.get().getWorkflowRunRef());
      //TODO: check if status is already completed or cancelled
    if (workflowRunEntity.isPresent()) {
      workflowExecutionClient.endRevision(workflowExecutionService, workflowRunEntity.get());
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  private List<TaskRun> getTaskRuns(String workflowRunId) {
    List<TaskRunEntity> runs = taskRunRepository.findByWorkflowRunRef(workflowRunId);
    List<TaskRun> taskRuns = new LinkedList<>();

    for (TaskRunEntity run : runs) {
      TaskRun tr = new TaskRun(run);
      taskRuns.add(tr);
    }
    return taskRuns;
  }

  private List<TaskExecutionResponse> getTaskExecutions(String workflowRunId) {
    List<TaskRunEntity> runs = taskRunRepository.findByWorkflowRunRef(workflowRunId);
    List<TaskExecutionResponse> taskExecutionResponses = new LinkedList<>();

    for (TaskRunEntity run : runs) {
      TaskExecutionResponse response = new TaskExecutionResponse();
      BeanUtils.copyProperties(run, response);
//
//      TODO: Update the following or make sure they are set on the run at execution end task time.
//      if (TaskType.approval.equals(run.getTaskType())
//          || TaskType.manual.equals(run.getTaskType())) {
//        Action approval = approvalService.getApprovalByTaskActivits(task.getId());
//        response.setApproval(approval);
//      } else if (TaskType.runworkflow == task.getTaskType()
//          && task.getRunWorkflowActivityId() != null) {
//
//        String runWorkflowActivityId = task.getRunWorkflowActivityId();
//        ActivityEntity activity =
//            this.flowActivityService.findWorkflowActivtyById(runWorkflowActivityId);
//        if (activity != null) {
//          response.setRunWorkflowActivityStatus(activity.getStatus());
//        }
//      } else if (TaskType.eventwait == task.getTaskType()) {
//        List<TaskOutputResult> results = new LinkedList<>();
//        TaskOutputResult result = new TaskOutputResult();
//        result.setName("eventPayload");
//        result.setDescription("Payload that was received with the Wait For Event");
//        if (task.getOutputs() != null) {
//          String json = task.getOutputs().get("eventPayload");
//          result.setValue(json);
//        }
//        results.add(result);
//        response.setResults(results);
//      } else if (TaskType.template == task.getTaskType()
//          || TaskType.customtask == task.getTaskType() || TaskType.script == task.getTaskType()) {
//        List<TaskOutputResult> results = new LinkedList<>();
//        setupTaskOutputResults(task, response, results);
//
//      }
      taskExecutionResponses.add(response);
    }
    return taskExecutionResponses;
  }

  private void logPayload(WorkflowExecutionRequest request) {
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
