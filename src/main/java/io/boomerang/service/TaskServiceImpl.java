package io.boomerang.service;

import static org.mockito.Mockito.timeout;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.history.Revision;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.InternalTaskResponse;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger();

//  @Autowired
//  @Lazy
//  private ControllerClient controllerClient;
//
//  @Autowired
//  private FlowWorkflowActivityService activityService;
//
//  @Autowired
//  private FlowWorkflowService workflowService;
//
//  @Autowired
//  private FlowTaskTemplateService templateService;
//
//  @Autowired
//  private RevisionService workflowVersionService;
//
//  @Autowired
//  private ActivityTaskService taskActivityService;

  @Autowired
  private DAGUtility dagUtility;

//  @Autowired
//  private Lock lock;
//
//  @Autowired
//  private ApprovalService approvalService;
//
//  @Autowired
//  private PropertyManager propertyManager;

//  @Autowired
//  private LockManager lockManager;

  @Autowired
  private TaskClient flowClient;

  @Autowired
  private WorkflowRunRepository workflowRunRepository;
  
  @Autowired
  private TaskRunRepository taskRunRepository;
//
//  @Autowired
//  private WorkflowScheduleService scheduleService;
  
  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Override
  @Async("flowAsyncExecutor")
  public void createTask(TaskExecution taskExecution) {
    LOGGER.debug("[{}] Recieved creating task request", taskExecution.getRunRef());
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());

    if (!wfRunEntity.isPresent() || RunStatus.cancelled.equals(wfRunEntity.get().getStatus())) {
      LOGGER.error("[{}] Workflow has been marked as cancelled, not starting task.",
          wfRunEntity.get().getId());
      return;
    }

    Optional<TaskRunEntity> optTaskRunEntity = taskRunRepository.findById(taskExecution.getRunRef());
    if (!optTaskRunEntity.isPresent() || !RunStatus.notstarted.equals(optTaskRunEntity.get().getStatus())) {
      LOGGER.debug("Task is null or hasn't started yet");
      return;
    }

    String workflowName = taskExecution.getWorkflowName();
    
    TaskRunEntity taskRunEntity = optTaskRunEntity.get();
    TaskType taskType = taskExecution.getType();
    taskRunEntity.setStartTime(new Date());
    taskRunEntity.setStatus(RunStatus.inProgress);
    taskRunEntity = taskRunRepository.save(taskRunEntity);


    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskExecution> tasks = dagUtility.createTaskList(workflowName, wfRevisionEntity.get(), wfRunEntity.get());
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);

    String wfRunId = wfRunEntity.get().getId();
    String taskId = taskExecution.getId();

    LOGGER.debug("[{}] Examining task type: {}", taskExecution.getRunRef(), taskType);

    if (canRunTask) {
      LOGGER.debug("[{}] Can run task? {}", taskExecution.getRunRef(), taskId);

      if (TaskType.decision.equals(taskType)) {
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(RunStatus.completed);
//        processDecision(taskRunEntity, wfRunId);
        this.endTask(response);
      } else if (TaskType.template.equals(taskType) || TaskType.script.equals(taskType)) {
        Map<String, String> labels = wfRunEntity.get().getLabels();
        //TODO: how do we submit the tasks
//        controllerClient.submitTemplateTask(this, flowClient, task, wfRunId, workflowName, labels);
        LOGGER.info("TODO - Execute Template Task");
      } else if (TaskType.customtask.equals(taskType)) {
        Map<String, String> labels = wfRunEntity.get().getLabels();
        //TODO: how do we submit the tasks
//        controllerClient.submitCustomTask(this, flowClient, task, wfRunId, workflowName, labels);
        LOGGER.info("TODO - Execute Custom Task");
      } else if (TaskType.acquirelock.equals(taskType)) {
        LOGGER.info("TODO - Create Lock");
//        createLock(task, activity);
      } else if (TaskType.releaselock.equals(taskType)) {
        LOGGER.info("TODO - Release Lock");
//        releaseLock(task, activity);
      } else if (TaskType.runworkflow.equals(taskType)) {
        LOGGER.info("TODO - Run Workflow");
//        this.runWorkflow(taskExecution, wfRunEntity.get());
      } else if (TaskType.runscheduledworkflow.equals(taskType)) {
        LOGGER.info("TODO - Run Scheduled Workflow");
//        this.runScheduledWorkflow(taskExecution, wfRunEntity.get(), workflowName);
      } else if (TaskType.setwfstatus.equals(taskType)) {
        LOGGER.info("Save Workflow Status");
        saveWorkflowStatus(taskExecution, wfRunEntity.get());
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(RunStatus.completed);
        this.endTask(response);
      } else if (TaskType.setwfproperty.equals(taskType)) {
        LOGGER.info("TODO - Save Workflow Property");
//        saveWorkflowProperty(taskExecution, wfRunEntity.get());
        InternalTaskResponse response = new InternalTaskResponse();
        response.setActivityId(taskExecution.getId());
        response.setStatus(RunStatus.completed);
        this.endTask(response);
      } else if (TaskType.approval.equals(taskType)) {
        LOGGER.info("TODO - Create Approval");
//        createApprovalNotification(taskExecution, task, activity, workflow, ManualType.approval);
      } else if (TaskType.manual.equals(taskType)) {
        LOGGER.info("TODO - Create Manual Action");
//        createApprovalNotification(taskExecution, task, activity, workflow, ManualType.task);
      } else if (TaskType.eventwait.equals(taskType)) {
        LOGGER.info("TODO - Wait for Event");
//        createWaitForEventTask(taskExecution);
      }
    } else {
      LOGGER.debug("[{}] Skipping task", taskId);
      InternalTaskResponse response = new InternalTaskResponse();
      response.setStatus(RunStatus.skipped);
      response.setActivityId(taskExecution.getId());

      endTask(response);
    }
  }

  private void saveWorkflowStatus(TaskExecution task, WorkflowRunEntity wfRunEntity) {
    String status = task.getParams().get("status").toString();
    if (!status.isBlank()) {
      RunStatus taskStatus = RunStatus.valueOf(status);
      wfRunEntity.setStatusOverride(taskStatus);
      this.workflowRunRepository.save(wfRunEntity);
    }
  }

//  private void processDecision(TaskRunEntity taskRunEntity, String activityId) {
//    String decisionValue = taskRunEntity.getDecisionValue();
////    ControllerRequestProperties properties =
////        propertyManager.buildRequestPropertyLayering(task, activityId, task.getWorkflowId());
//    String value = decisionValue;
////    value = propertyManager.replaceValueWithProperty(value, activityId, properties);
//    taskExecution.setDecisionValue(value);
//    taskRunEntity
//    TaskExecutionEntity taskExecution = taskActivityService.findById(task.getTaskActivityId());
//    taskExecution.setDecisionValue(value);
//    taskRunEntity = taskRunRepository.save(taskRunEntity);
//  }

//  private void releaseLock(Task task, ActivityEntity activity) {
//
//    LOGGER.debug("[{}] Releasing lock: ", task.getTaskActivityId());
//
//    lockManager.releaseLock(task, activity.getId());
//    InternalTaskResponse response = new InternalTaskResponse();
//    response.setActivityId(task.getTaskActivityId());
//    response.setStatus(TaskStatus.completed);
//    this.endTask(response);
//  }

//  private void runWorkflow(Task task, ActivityEntity activity) {
//
//    if (task.getInputs() != null) {
//      RequestFlowExecution request = new RequestFlowExecution();
//      request.setWorkflowId(task.getInputs().get("workflowId"));
//      Map<String, String> properties = new HashMap<>();
//      for (Map.Entry<String, String> entry : task.getInputs().entrySet()) {
//        if (!"workflowId".equals(entry.getKey())) {
//          properties.put(entry.getKey(), entry.getValue());
//        }
//      }
//
//      request.setProperties(properties);
//      String workflowActivityId = flowClient.submitWebhookEvent(request);
//      if (workflowActivityId != null) {
//        TaskExecutionEntity taskExecution = taskActivityService.findById(task.getTaskActivityId());
//        taskExecution.setRunWorkflowActivityId(workflowActivityId);
//        taskExecution.setRunWorkflowId(request.getWorkflowId());
//        taskActivityService.save(taskExecution);
//      }
//    }
//
//    InternalTaskResponse response = new InternalTaskResponse();
//    response.setActivityId(task.getTaskActivityId());
//    response.setStatus(TaskStatus.completed);
//    this.endTask(response);
//  }
//
//  private void runScheduledWorkflow(Task task, ActivityEntity activity, String workflowName) {
//    InternalTaskResponse response = new InternalTaskResponse();
//    response.setActivityId(task.getTaskActivityId());
//    response.setStatus(TaskStatus.failure);
//
//    if (task.getInputs() != null) {
//      String workflowId = task.getInputs().get("workflowId");
//      Integer futureIn = Integer.valueOf(task.getInputs().get("futureIn"));
//      String futurePeriod = task.getInputs().get("futurePeriod");
//      Date executionDate = activity.getCreationDate();
//      String timezone = task.getInputs().get("timezone");
//      LOGGER.debug("*******Run Scheduled Workflow System Task******");
//      LOGGER.debug("Scheduling new task in " + futureIn + " " + futurePeriod);
//
//      if (futureIn != null && futureIn != 0 && StringUtils.indexOfAny(futurePeriod,
//          new String[] {"minutes", "hours", "days", "weeks", "months"}) >= 0) {
//        Calendar executionCal = Calendar.getInstance();
//        executionCal.setTime(executionDate);
//        Integer calField = Calendar.MINUTE;
//        switch (futurePeriod) {
//          case "hours":
//            calField = Calendar.HOUR;
//            break;
//          case "days":
//            calField = Calendar.DATE;
//            break;
//          case "weeks":
//            futureIn = futureIn * 7;
//            calField = Calendar.DATE;
//            break;
//          case "months":
//            calField = Calendar.MONTH;
//            break;
//        }
//        executionCal.add(calField, futureIn);
//        if (!futurePeriod.equals("minutes") && !futurePeriod.equals("hours")) {
//          String[] hoursTime = task.getInputs().get("time").split(":");
//          Integer hours = Integer.valueOf(hoursTime[0]);
//          Integer minutes = Integer.valueOf(hoursTime[1]);
//          LOGGER
//              .debug("With time to be set to: " + task.getInputs().get("time") + " in " + timezone);
//          executionCal.setTimeZone(TimeZone.getTimeZone(timezone));
//          executionCal.set(Calendar.HOUR, hours);
//          executionCal.set(Calendar.MINUTE, minutes);
//          LOGGER.debug(
//              "With execution set to: " + executionCal.getTime().toString() + " in " + timezone);
//          executionCal.setTimeZone(TimeZone.getTimeZone("UTC"));
//        }
//        LOGGER.debug("With execution set to: " + executionCal.getTime().toString() + " in UTC");
//
//        // Define new properties removing the System Task specific properties
//        ControllerRequestProperties requestProperties = propertyManager
//            .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
//
//        Map<String, String> properties = new HashMap<>();
//        for (Map.Entry<String, String> entry : task.getInputs().entrySet()) {
//          if (!"workflowId".equals(entry.getKey()) && !"futureIn".equals(entry.getKey())
//              && !"futurePeriod".equals(entry.getKey()) && !"time".equals(entry.getKey())
//              && !"timezone".equals(entry.getKey())) {
//            String value = entry.getValue();
//            if (value != null) {
//              value = propertyManager.replaceValueWithProperty(value, activity.getId(),
//                  requestProperties);
//            }
//            properties.put(entry.getKey(), value);
//          }
//        }
//
//        // Define and create the schedule
//        WorkflowSchedule schedule = new WorkflowSchedule();
//        schedule.setWorkflowId(workflowId);
//        schedule.setName(task.getTaskName());
//        schedule
//            .setDescription("This schedule was generated through a Run Scheduled Workflow task.");
//        schedule.setParametersMap(properties);
//        schedule.setCreationDate(activity.getCreationDate());
//        schedule.setDateSchedule(executionCal.getTime());
//        schedule.setTimezone(timezone);
//        schedule.setType(WorkflowScheduleType.runOnce);
//        List<KeyValuePair> labels = new LinkedList<>();
//        labels.add(new KeyValuePair("workflowName", workflowName));
//        schedule.setLabels(labels);
//        WorkflowSchedule workflowSchedule = scheduleService.createSchedule(schedule);
//        if (workflowSchedule != null && workflowSchedule.getId() != null) {
//          LOGGER.debug("Workflow Scheudle (" + workflowSchedule.getId() + ") created.");
//          // TODO: Add a taskExecution with the ScheduleId so it can be deep linked.
//          response.setStatus(TaskStatus.completed);
//        }
//      }
//    }
//
//    this.endTask(response);
//  }
//
//  private void createLock(Task task, ActivityEntity activity) {
//
//    LOGGER.debug("[{}] Creating lock: ", task.getTaskActivityId());
//
//    lockManager.acquireLock(task, activity.getId());
//
//    LOGGER.debug("[{}] Finishing lock: ", task.getTaskActivityId());
//
//    InternalTaskResponse response = new InternalTaskResponse();
//    response.setActivityId(task.getTaskActivityId());
//    response.setStatus(TaskStatus.completed);
//    this.endTask(response);
//  }

//  private void createWaitForEventTask(TaskExecutionEntity taskExecution) {
//
//    LOGGER.debug("[{}] Creating wait for event task", taskExecution.getActivityId());
//
//    taskExecution.setFlowTaskStatus(TaskStatus.waiting);
//    taskActivityService.save(taskExecution);
//
//    if (taskExecution.isPreApproved()) {
//      InternalTaskResponse response = new InternalTaskResponse();
//      response.setActivityId(taskExecution.getId());
//      response.setStatus(TaskStatus.completed);
//      this.endTask(response);
//    }
//  }

////  private void createApprovalNotification(TaskExecutionEntity taskExecution, Task task,
////      ActivityEntity activity, WorkflowEntity workflow, ManualType type) {
////    taskExecution.setFlowTaskStatus(TaskStatus.waiting);
////    taskExecution = taskActivityService.save(taskExecution);
////    ApprovalEntity approval = new ApprovalEntity();
////    approval.setTaskActivityId(taskExecution.getId());
////    approval.setActivityId(activity.getId());
////    approval.setWorkflowId(workflow.getId());
////    approval.setTeamId(workflow.getFlowTeamId());
////    approval.setStatus(ApprovalStatus.submitted);
////    approval.setType(type);
////    approval.setCreationDate(new Date());
////    approval.setNumberOfApprovers(1);
////
////    if (ManualType.approval == type) {
////      if (task.getInputs() != null) {
////        String approverGroupId = task.getInputs().get("approverGroupId");
////        String numberOfApprovers = task.getInputs().get("numberOfApprovers");
////
////        if (approverGroupId != null && !approverGroupId.isBlank()) {
////          approval.setApproverGroupId(approverGroupId);
////        }
////        if (numberOfApprovers != null && !numberOfApprovers.isBlank()) {
////          approval.setNumberOfApprovers(Integer.valueOf(numberOfApprovers));
////        }
////      }
////    }
////    approvalService.save(approval);
////    activity.setAwaitingApproval(true);
////    this.activityService.saveWorkflowActivity(activity);
////  }
////
////  private void saveWorkflowProperty(Task task, ActivityEntity activity,
////      TaskExecutionEntity taskEntity) {
////    if (taskEntity.getOutputProperties() == null) {
////      taskEntity.setOutputProperties(new LinkedList<>());
////    }
////
////    String input = task.getInputs().get("value");
////    String output = task.getInputs().get("output");
////
////    List<KeyValuePair> outputProperties = taskEntity.getOutputProperties();
////
////    KeyValuePair outputProperty = new KeyValuePair();
////    outputProperty.setKey(output);
////
////    ControllerRequestProperties requestProperties = propertyManager
////        .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
////    String outputValue =
////        propertyManager.replaceValueWithProperty(input, activity.getId(), requestProperties);
////
////    outputProperty.setValue(outputValue);
////    outputProperties.add(outputProperty);
////    taskEntity.setOutputProperties(outputProperties);
////    taskActivityService.save(taskEntity);
////
////  }
//
//  @Override
//  @Async("flowAsyncExecutor")
//  public void endTask(InternalTaskResponse request) {
//
//    String activityId = request.getActivityId();
//    LOGGER.info("[{}] Recieved end task request", activityId);
//    TaskExecutionEntity activity = taskActivityService.findById(activityId);
//
//    ActivityEntity workflowActivity =
//        this.activityService.findWorkflowActivtyById(activity.getActivityId());
//
//    if (workflowActivity.getStatus() == TaskStatus.cancelled) {
//      LOGGER.error("[{}] Workflow has been marked as cancelled, not ending task", activityId);
//      activity.setFlowTaskStatus(TaskStatus.cancelled);
//      long duration = new Date().getTime() - activity.getStartTime().getTime();
//      activity.setDuration(duration);
//      taskActivityService.save(activity);
//      return;
//    }
//
//    RevisionEntity revision =
//        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
//    Task currentTask = getTask(activity);
//    List<Task> tasks = this.createTaskList(revision, workflowActivity);
//
//    String storeId = workflowActivity.getId();
//
//    List<String> keys = new LinkedList<>();
//    keys.add(storeId);
//
//    workflowActivity = this.activityService.findWorkflowActivtyById(activity.getActivityId());
//
//
//    activity.setFlowTaskStatus(request.getStatus());
//    long duration = new Date().getTime() - activity.getStartTime().getTime();
//    activity.setDuration(duration);
//
//
//    if (request.getOutputProperties() != null && !request.getOutputProperties().isEmpty()) {
//      activity.setOutputs(request.getOutputProperties());
//    }
//
//    activity = taskActivityService.save(activity);
//
//    boolean finishedAll = this.finishedAll(workflowActivity, tasks, currentTask);
//
//    LOGGER.debug("[{}] Finished all previous tasks? {}", activityId, finishedAll);
//
//
//    LOGGER.debug("[{}] Attempting to get lock", activityId);
//    String tokenId = getLock(storeId, keys, 105000);
//    LOGGER.debug("[{}] Obtained lock", activityId);
//
//    workflowActivity = this.activityService.findWorkflowActivtyById(activity.getActivityId());
//    updatePendingAprovalStatus(workflowActivity);
//
//    activity.setFlowTaskStatus(request.getStatus());
//
//    String workflowActivityId = workflowActivity.getId();
//
//    if (this.flowActivityService.hasExceededExecutionQuotas(workflowActivityId)) {
//      LOGGER.error("Workflow has been cancelled due to its max workflow duration has exceeded.");
//      ErrorResponse response = new ErrorResponse();
//      response
//          .setMessage("Workflow execution terminated due to exceeding maxinum workflow duration.");
//      response.setCode("001");
//
//      this.flowActivityService.cancelWorkflowActivity(workflowActivityId, response);
//    } else {
//      executeNextStep(workflowActivity, tasks, currentTask, finishedAll);
//    }
//    lock.release(keys, "locks", tokenId);
//    LOGGER.debug("[{}] Released lock", activityId);
//  }
//
//  private void updatePendingAprovalStatus(ActivityEntity workflowActivity) {
//    long count = approvalService.getApprovalCountForActivity(workflowActivity.getId(),
//        ApprovalStatus.submitted);
//    boolean existingApprovals = (count > 0);
//    workflowActivity.setAwaitingApproval(existingApprovals);
//    this.activityService.saveWorkflowActivity(workflowActivity);
//  }
//
//  private String getLock(String storeId, List<String> keys, long timeout) {
//    RetryTemplate retryTemplate = getRetryTemplate();
//    return retryTemplate.execute(ctx -> {
//      final String token = lock.acquire(keys, "locks", timeout);
//      if (StringUtils.isEmpty(token)) {
//        throw new LockNotAvailableException(
//            String.format("Lock not available for keys: %s in store %s", keys, storeId));
//      }
//      return token;
//    });
//  }
//
//  private RetryTemplate getRetryTemplate() {
//    RetryTemplate retryTemplate = new RetryTemplate();
//    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
//    fixedBackOffPolicy.setBackOffPeriod(2000l);
//    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
//
//    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
//    retryPolicy.setMaxAttempts(100);
//    retryTemplate.setRetryPolicy(retryPolicy);
//    return retryTemplate;
//  }
//
//  private void finishWorkflow(ActivityEntity activity) {
//
//    WorkflowEntity workflow = workflowService.getWorkflow(activity.getWorkflowId());
//
//    this.controllerClient.terminateFlow(workflow.getId(), workflow.getName(), activity.getId());
//    boolean workflowCompleted = dagUtility.validateWorkflow(activity);
//
//    if (activity.getStatusOverride() != null) {
//      activity.setStatus(activity.getStatusOverride());
//    } else {
//      if (workflowCompleted) {
//        activity.setStatus(TaskStatus.completed);
//      } else {
//        activity.setStatus(TaskStatus.failure);
//      }
//    }
//
//    final Date finishDate = new Date();
//    final long duration = finishDate.getTime() - activity.getCreationDate().getTime();
//    activity.setDuration(duration);
//
//    List<TaskExecutionEntity> taskExecutions =
//        taskActivityService.findTaskActiivtyForActivity(activity.getId());
//    if (activity.getOutputProperties() == null) {
//      activity.setOutputProperties(new LinkedList<>());
//    }
//
//    for (TaskExecutionEntity taskExecution : taskExecutions) {
//      if (taskExecution.getOutputProperties() != null) {
//        activity.getOutputProperties().addAll(taskExecution.getOutputProperties());
//      }
//    }
//
//    this.activityService.saveWorkflowActivity(activity);
//
//  }
//
//  private void executeNextStep(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask,
//      boolean finishedAll) {
//    LOGGER.debug("[{}] Looking at next tasks", workflowActivity.getId());
//    LOGGER.debug("Testing at next tasks");
//    List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
//    LOGGER.debug("Testing at next tasks: {}", nextNodes.size());
//
//    for (Task next : nextNodes) {
//
//      if (next.getTaskType() == TaskType.end) {
//        if (finishedAll) {
//          LOGGER.debug("FINISHED ALL");
//          this.finishWorkflow(workflowActivity);
//          return;
//        }
//        continue;
//      }
//
//      boolean executeTask = canExecuteTask(workflowActivity, next);
//      LOGGER.debug("[{}] Task: {}", workflowActivity.getId(), next.getTaskName());
//
//
//      if (executeTask) {
//        TaskExecutionEntity task = this.taskActivityService
//            .findByTaskIdAndActivityId(next.getTaskId(), workflowActivity.getId());
//        if (task == null) {
//          LOGGER.debug("Reached node which should not be executed.");
//        } else {
//          InternalTaskRequest taskRequest = new InternalTaskRequest();
//          taskRequest.setActivityId(task.getId());
//          flowClient.startTask(this, taskRequest);
//        }
//      }
//    }
//  }
//
//  private boolean finishedAll(ActivityEntity workflowActivity, List<Task> tasks, Task currentTask) {
//    boolean finishedAll = true;
//
//    List<Task> nextNodes = this.getTasksDependants(tasks, currentTask);
//    for (Task next : nextNodes) {
//      if (next.getTaskType() == TaskType.end) {
//        List<String> deps = next.getDependencies();
//        for (String dep : deps) {
//          TaskExecutionEntity task =
//              this.taskActivityService.findByTaskIdAndActivityId(dep, workflowActivity.getId());
//          if (task == null) {
//            continue;
//          }
//
//          TaskStatus status = task.getFlowTaskStatus();
//          if (status == TaskStatus.inProgress || status == TaskStatus.notstarted
//              || status == TaskStatus.waiting) {
//            finishedAll = false;
//          }
//        }
//      }
//    }
//
//    return finishedAll;
//  }
//
//  private boolean canExecuteTask(ActivityEntity workflowActivity, Task next) {
//    List<String> deps = next.getDependencies();
//    for (String dep : deps) {
//      TaskExecutionEntity task =
//          taskActivityService.findByTaskIdAndActivityId(dep, workflowActivity.getId());
//      if (task != null) {
//        TaskStatus status = task.getFlowTaskStatus();
//        if (status == TaskStatus.inProgress || status == TaskStatus.notstarted
//            || status == TaskStatus.waiting) {
//          return false;
//        }
//      }
//    }
//    return true;
//  }
//
//  private List<Task> getTasksDependants(List<Task> tasks, Task currentTask) {
//    return tasks.stream().filter(c -> c.getDependencies().contains(currentTask.getTaskId()))
//        .collect(Collectors.toList());
//  }
//
//  private Task getTask(TaskExecutionEntity taskActivity) {
//    ActivityEntity activity = activityService.findWorkflowActivtyById(taskActivity.getActivityId());
//    RevisionEntity revision =
//        workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
//    List<Task> tasks = createTaskList(revision, activity);
//    String taskId = taskActivity.getTaskId();
//    return tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
//  }
//
//  @Override
//  public List<String> updateTaskActivityForTopic(String activityId, String topic) {
//
//    List<String> ids = new LinkedList<>();
//
//    LOGGER.info("[{}] Fidning task actiivty id based on topic.", activityId);
//    ActivityEntity activity = activityService.findWorkflowActivtyById(activityId);
//    RevisionEntity revision =
//        workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
//
//
//    List<DAGTask> tasks = revision.getDag().getTasks();
//    for (DAGTask task : tasks) {
//      if (TaskType.eventwait.equals(task.getType())) {
//        List<KeyValuePair> coreProperties = task.getProperties();
//        if (coreProperties != null) {
//          KeyValuePair coreProperty = coreProperties.stream()
//              .filter(c -> "topic".contains(c.getKey())).findAny().orElse(null);
//
//          if (coreProperty != null && topic.equals(coreProperty.getValue())) {
//
//            String text = coreProperty.getValue();
//            ControllerRequestProperties properties = propertyManager
//                .buildRequestPropertyLayering(null, activityId, activity.getWorkflowId());
//            text = propertyManager.replaceValueWithProperty(text, activityId, properties);
//
//            String taskId = task.getTaskId();
//            TaskExecutionEntity taskExecution =
//                this.taskActivityService.findByTaskIdAndActivityId(taskId, activityId);
//            if (taskExecution != null) {
//              LOGGER.info("[{}] Found task id: {} ", activityId, taskExecution.getId());
//              taskExecution.setPreApproved(true);
//              this.taskActivityService.save(taskExecution);
//
//              ids.add(taskExecution.getId());
//            }
//          }
//        }
//      }
//    }
//    LOGGER.info("[{}] No task activity ids found for topic: {}", activityId, topic);
//    return ids;
//  }
//
//  @Override
//  @Async("flowAsyncExecutor")
//  public void submitActivity(String taskActivityId, String taskStatus,
//      Map<String, String> outputProperties) {
//
//    LOGGER.info("submitActivity: {}", taskStatus);
//
//    TaskStatus status = TaskStatus.completed;
//    if ("success".equals(taskStatus)) {
//      status = TaskStatus.completed;
//    } else if ("failure".equals(taskStatus)) {
//      status = TaskStatus.failure;
//    }
//
//    LOGGER.info("Submit Activity (Task Status): {}", status.toString());
//
//
//    TaskExecutionEntity taskExecution = this.taskActivityService.findById(taskActivityId);
//    if (taskExecution != null && !taskExecution.getFlowTaskStatus().equals(TaskStatus.notstarted)) {
//      InternalTaskResponse request = new InternalTaskResponse();
//      request.setActivityId(taskActivityId);
//      request.setStatus(status);
//
//      if (outputProperties != null) {
//        request.setOutputProperties(outputProperties);
//      }
//
//      endTask(request);
//    }
//  }
}
