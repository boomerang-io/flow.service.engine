package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.enums.RunStatus;

@Service
public class WorkflowRunServiceImpl implements WorkflowRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Override
  public WorkflowRunEntity createRun(WorkflowRevisionEntity revision,
      WorkflowExecutionRequest request, Map<String, String> labels) {
    
    Optional<WorkflowEntity> workflow = workflowRepository.findById(revision.getWorkflowRef());

    if (!workflow.isPresent()) {
//      throw new exception
    }
    final WorkflowRunEntity workflowRun = new WorkflowRunEntity();
    workflowRun.setWorkflowRevisionRef(revision.getId());
    workflowRun.setWorkflowRef(revision.getWorkflowRef());
    workflowRun.setCreationDate(new Date());
    workflowRun.setStatus(RunStatus.notstarted);
    workflowRun.putLabels(workflow.get().getLabels());
    workflowRun.putLabels(request.getLabels());
    workflowRun.setParams(request.getParams());
    
    //TODO: add trigger and set initiatedBy
//    workflowRun.setTrigger(null);
//    if (!trigger.isPresent() || "manual".equals(trigger.get())) {
//      final UserEntity userEntity = userIdentityService.getCurrentUser();
//      activity.setInitiatedById(userEntity.getId());
//    }
    
    //TODO: add resources
//    workflowRun.setResources(null);
    
    return workflowRunRepository.save(workflowRun);
  }

  @Override
  public List<TaskExecutionResponse> getTaskExecutions(String workflowRunId) {
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
}
