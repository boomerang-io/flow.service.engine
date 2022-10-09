package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.model.WorkflowRevisionTask;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.RunResult;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;

@Service
public class TaskExecutionServiceImpl implements TaskExecutionService {

  private static final Logger LOGGER = LogManager.getLogger(TaskExecutionServiceImpl.class);

  // @Autowired
  // @Lazy
  // private ControllerClient controllerClient;
  //
  // @Autowired
  // private FlowWorkflowActivityService activityService;
  //
  // @Autowired
  // private FlowWorkflowService workflowService;
  //
  // @Autowired
  // private FlowTaskTemplateService templateService;
  //
  // @Autowired
  // private RevisionService workflowVersionService;
  //
  // @Autowired
  // private ActivityTaskService taskActivityService;

  @Autowired
  private DAGUtility dagUtility;

  @Autowired
  private Lock lock;
  //
  // @Autowired
  // private ApprovalService approvalService;
  //
  // @Autowired
  // private PropertyManager propertyManager;

  @Autowired
  @Lazy
  private LockManager lockManager;

  @Autowired
  private TaskExecutionClient flowClient;

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;
  //
  // @Autowired
  // private WorkflowScheduleService scheduleService;

  @Value("${flow.engine.mode}")
  private String engineMode;

  @Override
  @Async("asyncTaskExecutor")
  public void queueTask(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.debug("[{}] Recieved queue task request.", taskExecutionId);

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find Workflow Run"));
      return;
    } else if (RunPhase.completed.equals(wfRunEntity.get().getPhase())
        || RunPhase.finalized.equals(wfRunEntity.get().getPhase())) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("[{}] Workflow has been marked as {}, unable to queue task."),
          taskExecutionId, wfRunEntity.get().getStatus());
      return;
    }

    // Check if TaskRun Phase is valid
    if (!RunPhase.pending.equals(taskExecution.getPhase())) {
      LOGGER.debug("[{}] Task Status invalid.", taskExecutionId);
      return;
    }

    // Ensure Task is valid as part of Graph
    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get().getId());
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);
    LOGGER.debug("[{}] Can run task? {}", taskExecutionId, canRunTask);

    if (canRunTask) {
      // Update Status and Phase
      updateStatusAndSaveTask(taskExecution, RunStatus.ready, RunPhase.pending, Optional.empty());

      // If in sync mode, don't wait for external prompt to startTask
      if ("sync".equals(engineMode)) {
        startTask(taskExecution);
      }
    } else {
      LOGGER.debug("[{}] Skipping task: {}", taskExecutionId, taskExecution.getName());
      taskExecution.setStatus(RunStatus.skipped);
      endTask(taskExecution);
    }
  }

  @Override
  @Async("asyncTaskExecutor")
  public void startTask(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.info("[{}] Recieved start task request.", taskExecutionId);
    LOGGER.debug("[{}] TaskRun: {}", taskExecutionId, taskExecution.toString());

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find Workflow Run"));
      return;
    } else if (RunPhase.completed.equals(wfRunEntity.get().getPhase())
        || RunPhase.finalized.equals(wfRunEntity.get().getPhase())) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed, Optional.of(
          "[{}] Workflow has been marked as {}. Setting task as Cancelled. Task may still run to completion."),
          wfRunEntity.get().getId(), wfRunEntity.get().getStatus());
      return;
    }

    // Check if TaskRun Phase is valid. Pending means it correctly came from queueTask();
    if (!RunPhase.pending.equals(taskExecution.getPhase())) {
      LOGGER.debug("[{}] Task Status invalid.", taskExecutionId);
      return;
    }

    String wfRunId = wfRunEntity.get().getId();
    // Ensure Task is valid as part of Graph
    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity.get(), wfRunId);
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);
    LOGGER.debug("[{}] Can run task? {}", taskExecutionId, canRunTask);

    // Execute based on TaskType
    TaskType taskType = taskExecution.getType();
    LOGGER.debug("[{}] Examining task type: {}", taskExecutionId, taskType);
    if (canRunTask) {
      // Set up task
      taskExecution.setStartTime(new Date());
      updateStatusAndSaveTask(taskExecution, RunStatus.running, RunPhase.running, Optional.empty());
      if (TaskType.decision.equals(taskType)) {
        taskExecution.setStatus(RunStatus.succeeded);
        processDecision(taskExecution, wfRunId);
        this.endTask(taskExecution);
      } else if (TaskType.template.equals(taskType) || TaskType.script.equals(taskType)) {
        // Map<String, String> labels = wfRunEntity.get().getLabels();
        // TODO: how do we submit the tasks
        LOGGER.info("[{}] Execute Template Task", wfRunId);
        // controllerClient.submitTemplateTask(this, flowClient, task, wfRunId, workflowName,
        // labels);
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.customtask.equals(taskType)) {
        // TODO: how do we submit the tasks
        LOGGER.info("[{}] Execute Custom Task", wfRunId);
        // Map<String, String> labels = wfRunEntity.get().getLabels();
        // controllerClient.submitCustomTask(this, flowClient, task, wfRunId, workflowName, labels);
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.acquirelock.equals(taskType)) {
        LOGGER.info("[{}] Execute Acquire Lock", wfRunId);
        lockManager.acquireLock(taskExecution, wfRunEntity.get().getId());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.releaselock.equals(taskType)) {
        LOGGER.info("[{}] Execute Release Lock", wfRunId);
        lockManager.releaseLock(taskExecution, wfRunEntity.get().getId());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.runworkflow.equals(taskType)) {
        LOGGER.info("TODO - Run Workflow");
        // this.runWorkflow(taskExecution, wfRunEntity.get());
      } else if (TaskType.runscheduledworkflow.equals(taskType)) {
        LOGGER.info("TODO - Run Scheduled Workflow");
        // this.runScheduledWorkflow(taskExecution, wfRunEntity.get(), workflowName);
      } else if (TaskType.setwfstatus.equals(taskType)) {
        LOGGER.info("[{}] Save Workflow Status", wfRunId);
        saveWorkflowStatus(taskExecution, wfRunEntity.get());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.setwfproperty.equals(taskType)) {
        LOGGER.info("TODO - Save Workflow Property");
        // saveWorkflowProperty(taskExecution, wfRunEntity.get());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.approval.equals(taskType)) {
        LOGGER.info("TODO - Create Approval");
        // createApprovalNotification(taskExecution, task, activity, workflow, ManualType.approval);
      } else if (TaskType.manual.equals(taskType)) {
        LOGGER.info("TODO - Create Manual Action");
        // createApprovalNotification(taskExecution, task, activity, workflow, ManualType.task);
      } else if (TaskType.eventwait.equals(taskType)) {
        LOGGER.info("TODO - Wait for Event");
        // createWaitForEventTask(taskExecution);
      }
    } else {
      LOGGER.debug("[{}] Skipping task: {}", taskExecutionId, taskExecution.getName());
      taskExecution.setStatus(RunStatus.skipped);
      endTask(taskExecution);
    }
  }

  @Override
  @Async("asyncTaskExecutor")
  public void endTask(TaskRunEntity taskExecution) {
    String taskRunId = taskExecution.getId();
    LOGGER.info("[{}] Recieved end task request.", taskRunId);

    // Check if task has been previously completed or cancelled
    TaskRunEntity storedTaskRun = taskRunRepository.findById(taskExecution.getId()).get();
    if (RunPhase.completed.equals(storedTaskRun.getPhase())) {
      LOGGER.error("[{}] Task has already been completed or cancelled.", taskRunId);
      return;
    }

    // Check if WorkflowRun status is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent() || RunStatus.cancelled.equals(wfRunEntity.get().getStatus())) {
      LOGGER.error(
          "[{}] Workflow has been marked as cancelled. Setting task as Cancelled. Task will still run to completion.",
          taskRunId);
      // TODO handle task being cancelled if in Queued. There will be no start time.
      taskExecution.setStatus(RunStatus.cancelled);
      taskExecution.setStatusMessage(
          "Workflow has been marked as cancelled. Setting task as Cancelled. Task will still run to completion.");
      long duration = new Date().getTime() - taskExecution.getStartTime().getTime();
      taskExecution.setDuration(duration);
      taskRunRepository.save(taskExecution);
      return;
    } else if (!wfRunEntity.isPresent() || RunStatus.failed.equals(wfRunEntity.get().getStatus())
        || RunStatus.invalid.equals(wfRunEntity.get().getStatus())) {
      LOGGER.error(
          "[{}] Workflow has been marked as failed or invalid. Setting task as Cancelled. Task will still run to completion.",
          taskRunId);
      // TODO handle task being cancelled if in Queued. There will be no start time.
      taskExecution.setStatus(RunStatus.cancelled);
      taskExecution.setStatusMessage(
          "Workflow has been marked as failed or invalid. Setting task as Cancelled. Task will still run to completion.");
      long duration = new Date().getTime() - taskExecution.getStartTime().getTime();
      taskExecution.setDuration(duration);
      taskRunRepository.save(taskExecution);
      return;
    }
    List<String> keys = new LinkedList<>();
    keys.add(taskRunId);

    // Update TaskRun with current TaskExecution status
    LOGGER.info("[{}] Marking Task as {}.", taskRunId, taskExecution.getStatus());
    long duration = taskExecution.getStartTime() != null ? new Date().getTime() - taskExecution.getStartTime().getTime() : 0;
    taskExecution.setPhase(RunPhase.completed);
    taskExecution.setDuration(duration);
    taskExecution = taskRunRepository.save(taskExecution);

    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get().getId());
    boolean finishedAllDependencies = this.finishedAll(wfRunEntity.get(), tasks, taskExecution);
    LOGGER.debug("[{}] Finished all previous tasks? {}", taskRunId, finishedAllDependencies);

    LOGGER.debug("[{}] Attempting to get lock", taskRunId);
    String tokenId = getLock(taskRunId, keys, 105000);
    LOGGER.debug("[{}] Obtained lock", taskRunId);

    // Refresh wfRunEntity and Execute Next Task
    wfRunEntity = this.workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    executeNextStep(wfRunEntity.get(), tasks, taskExecution, finishedAllDependencies);
    
    lock.release(keys, "locks", tokenId);
    LOGGER.debug("[{}] Released lock", taskRunId);
  }

  @Override
  public List<String> updateTaskRunForTopic(String workflowRunId, String topic) {
    List<String> ids = new LinkedList<>();

    LOGGER.info("[{}] Finding taskRunId based on topic.", workflowRunId);
    Optional<WorkflowRunEntity> workflowRunEntity = workflowRunRepository.findById(workflowRunId);
    Optional<WorkflowRevisionEntity> workflowRevisionEntity =
        workflowRevisionRepository.findById(workflowRunEntity.get().getWorkflowRevisionRef());

    if (workflowRevisionEntity.isPresent()) {
      List<WorkflowRevisionTask> tasks = workflowRevisionEntity.get().getTasks();
      for (WorkflowRevisionTask task : tasks) {
        if (TaskType.eventwait.equals(task.getType())) {
          Map<String, Object> params = task.getParams();
          if (params != null && params.containsKey("topic")) {
            // TODO: bring back parameter layering
//            String paramTopic = params.get("topic").toString();
            // ControllerRequestProperties properties = propertyManager
            // .buildRequestPropertyLayering(null, taskRunId, activity.getWorkflowId());
            // topic = propertyManager.replaceValueWithProperty(paramTopic, taskRunId, properties);
            // String taskId = task.getId();
            Optional<TaskRunEntity> taskRunEntity = this.taskRunRepository
                .findFirstByNameAndWorkflowRunRef(task.getName(), workflowRunId);
            if (taskRunEntity.isPresent() && taskRunEntity != null) {
              LOGGER.info("[{}] Found task run id: {} ", workflowRunId,
                  taskRunEntity.get().getId());
              taskRunEntity.get().setPreApproved(true);
              this.taskRunRepository.save(taskRunEntity.get());

              ids.add(taskRunEntity.get().getId());
            }
          }
        }
      }
    }
    LOGGER.info("[{}] No task activity ids found for topic: {}", workflowRunId, topic);
    return ids;
  }

  @Override
  @Async("asyncTaskExecutor")
  public void submitActivity(String taskRunId, String taskStatus, List<RunResult> results) {

    LOGGER.info("[{}] SubmitActivity: {}", taskRunId, taskStatus);

    RunStatus status = RunStatus.succeeded;
    if ("success".equals(taskStatus)) {
      status = RunStatus.succeeded;
    } else if ("failure".equals(taskStatus)) {
      status = RunStatus.failed;
    }

    Optional<TaskRunEntity> taskRunEntity = this.taskRunRepository.findById(taskRunId);
    if (taskRunEntity.isPresent()
        && !taskRunEntity.get().getStatus().equals(RunStatus.notstarted)) {
      taskRunEntity.get().setStatus(status);
      if (results != null) {
        taskRunEntity.get().setResults(results);
      }

      endTask(taskRunEntity.get());
    }
  }

  private void saveWorkflowStatus(TaskRunEntity task, WorkflowRunEntity wfRunEntity) {
    String status = task.getParams().get("status").toString();
    if (!status.isBlank()) {
      RunStatus taskStatus = RunStatus.valueOf(status);
      wfRunEntity.setStatusOverride(taskStatus);
      this.workflowRunRepository.save(wfRunEntity);
    }
  }

  private void processDecision(TaskRunEntity taskExecution, String activityId) {
    String decisionValue = taskExecution.getParams().get("value").toString();
    // ControllerRequestProperties properties =
    // propertyManager.buildRequestPropertyLayering(taskExecution, activityId,
    // task.getWorkflowId());
    String value = decisionValue;
    // value = propertyManager.replaceValueWithProperty(value, activityId, properties);
    taskExecution.setDecisionValue(value);
    taskRunRepository.save(taskExecution);
  }

  // private void runWorkflow(Task task, ActivityEntity activity) {
  //
  // if (task.getInputs() != null) {
  // RequestFlowExecution request = new RequestFlowExecution();
  // request.setWorkflowId(task.getInputs().get("workflowId"));
  // Map<String, String> properties = new HashMap<>();
  // for (Map.Entry<String, String> entry : task.getInputs().entrySet()) {
  // if (!"workflowId".equals(entry.getKey())) {
  // properties.put(entry.getKey(), entry.getValue());
  // }
  // }
  //
  // request.setProperties(properties);
  // String workflowActivityId = flowClient.submitWebhookEvent(request);
  // if (workflowActivityId != null) {
  // TaskExecutionEntity taskExecution = taskActivityService.findById(task.getTaskActivityId());
  // taskExecution.setRunWorkflowActivityId(workflowActivityId);
  // taskExecution.setRunWorkflowId(request.getWorkflowId());
  // taskActivityService.save(taskExecution);
  // }
  // }
  //
  // InternalTaskResponse response = new InternalTaskResponse();
  // response.setActivityId(task.getTaskActivityId());
  // response.setStatus(TaskStatus.completed);
  // this.endTask(response);
  // }
  //
  // private void runScheduledWorkflow(Task task, ActivityEntity activity, String workflowName) {
  // InternalTaskResponse response = new InternalTaskResponse();
  // response.setActivityId(task.getTaskActivityId());
  // response.setStatus(TaskStatus.failure);
  //
  // if (task.getInputs() != null) {
  // String workflowId = task.getInputs().get("workflowId");
  // Integer futureIn = Integer.valueOf(task.getInputs().get("futureIn"));
  // String futurePeriod = task.getInputs().get("futurePeriod");
  // Date executionDate = activity.getCreationDate();
  // String timezone = task.getInputs().get("timezone");
  // LOGGER.debug("*******Run Scheduled Workflow System Task******");
  // LOGGER.debug("Scheduling new task in " + futureIn + " " + futurePeriod);
  //
  // if (futureIn != null && futureIn != 0 && StringUtils.indexOfAny(futurePeriod,
  // new String[] {"minutes", "hours", "days", "weeks", "months"}) >= 0) {
  // Calendar executionCal = Calendar.getInstance();
  // executionCal.setTime(executionDate);
  // Integer calField = Calendar.MINUTE;
  // switch (futurePeriod) {
  // case "hours":
  // calField = Calendar.HOUR;
  // break;
  // case "days":
  // calField = Calendar.DATE;
  // break;
  // case "weeks":
  // futureIn = futureIn * 7;
  // calField = Calendar.DATE;
  // break;
  // case "months":
  // calField = Calendar.MONTH;
  // break;
  // }
  // executionCal.add(calField, futureIn);
  // if (!futurePeriod.equals("minutes") && !futurePeriod.equals("hours")) {
  // String[] hoursTime = task.getInputs().get("time").split(":");
  // Integer hours = Integer.valueOf(hoursTime[0]);
  // Integer minutes = Integer.valueOf(hoursTime[1]);
  // LOGGER
  // .debug("With time to be set to: " + task.getInputs().get("time") + " in " + timezone);
  // executionCal.setTimeZone(TimeZone.getTimeZone(timezone));
  // executionCal.set(Calendar.HOUR, hours);
  // executionCal.set(Calendar.MINUTE, minutes);
  // LOGGER.debug(
  // "With execution set to: " + executionCal.getTime().toString() + " in " + timezone);
  // executionCal.setTimeZone(TimeZone.getTimeZone("UTC"));
  // }
  // LOGGER.debug("With execution set to: " + executionCal.getTime().toString() + " in UTC");
  //
  // // Define new properties removing the System Task specific properties
  // ControllerRequestProperties requestProperties = propertyManager
  // .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
  //
  // Map<String, String> properties = new HashMap<>();
  // for (Map.Entry<String, String> entry : task.getInputs().entrySet()) {
  // if (!"workflowId".equals(entry.getKey()) && !"futureIn".equals(entry.getKey())
  // && !"futurePeriod".equals(entry.getKey()) && !"time".equals(entry.getKey())
  // && !"timezone".equals(entry.getKey())) {
  // String value = entry.getValue();
  // if (value != null) {
  // value = propertyManager.replaceValueWithProperty(value, activity.getId(),
  // requestProperties);
  // }
  // properties.put(entry.getKey(), value);
  // }
  // }
  //
  // // Define and create the schedule
  // WorkflowSchedule schedule = new WorkflowSchedule();
  // schedule.setWorkflowId(workflowId);
  // schedule.setName(task.getTaskName());
  // schedule
  // .setDescription("This schedule was generated through a Run Scheduled Workflow task.");
  // schedule.setParametersMap(properties);
  // schedule.setCreationDate(activity.getCreationDate());
  // schedule.setDateSchedule(executionCal.getTime());
  // schedule.setTimezone(timezone);
  // schedule.setType(WorkflowScheduleType.runOnce);
  // List<KeyValuePair> labels = new LinkedList<>();
  // labels.add(new KeyValuePair("workflowName", workflowName));
  // schedule.setLabels(labels);
  // WorkflowSchedule workflowSchedule = scheduleService.createSchedule(schedule);
  // if (workflowSchedule != null && workflowSchedule.getId() != null) {
  // LOGGER.debug("Workflow Scheudle (" + workflowSchedule.getId() + ") created.");
  // // TODO: Add a taskExecution with the ScheduleId so it can be deep linked.
  // response.setStatus(TaskStatus.completed);
  // }
  // }
  // }
  //
  // this.endTask(response);
  // }
  //

  // private void createWaitForEventTask(TaskExecutionEntity taskExecution) {
  //
  // LOGGER.debug("[{}] Creating wait for event task", taskExecution.getActivityId());
  //
  // taskExecution.setFlowTaskStatus(TaskStatus.waiting);
  // taskActivityService.save(taskExecution);
  //
  // if (taskExecution.isPreApproved()) {
  // InternalTaskResponse response = new InternalTaskResponse();
  // response.setActivityId(taskExecution.getId());
  // response.setStatus(TaskStatus.completed);
  // this.endTask(response);
  // }
  // }

  //// private void createApprovalNotification(TaskExecutionEntity taskExecution, Task task,
  //// ActivityEntity activity, WorkflowEntity workflow, ManualType type) {
  //// taskExecution.setFlowTaskStatus(TaskStatus.waiting);
  //// taskExecution = taskActivityService.save(taskExecution);
  //// ApprovalEntity approval = new ApprovalEntity();
  //// approval.setTaskActivityId(taskExecution.getId());
  //// approval.setActivityId(activity.getId());
  //// approval.setWorkflowId(workflow.getId());
  //// approval.setTeamId(workflow.getFlowTeamId());
  //// approval.setStatus(ApprovalStatus.submitted);
  //// approval.setType(type);
  //// approval.setCreationDate(new Date());
  //// approval.setNumberOfApprovers(1);
  ////
  //// if (ManualType.approval == type) {
  //// if (task.getInputs() != null) {
  //// String approverGroupId = task.getInputs().get("approverGroupId");
  //// String numberOfApprovers = task.getInputs().get("numberOfApprovers");
  ////
  //// if (approverGroupId != null && !approverGroupId.isBlank()) {
  //// approval.setApproverGroupId(approverGroupId);
  //// }
  //// if (numberOfApprovers != null && !numberOfApprovers.isBlank()) {
  //// approval.setNumberOfApprovers(Integer.valueOf(numberOfApprovers));
  //// }
  //// }
  //// }
  //// approvalService.save(approval);
  //// activity.setAwaitingApproval(true);
  //// this.activityService.saveWorkflowActivity(activity);
  //// }
  ////
  //// private void saveWorkflowProperty(Task task, ActivityEntity activity,
  //// TaskExecutionEntity taskEntity) {
  //// if (taskEntity.getOutputProperties() == null) {
  //// taskEntity.setOutputProperties(new LinkedList<>());
  //// }
  ////
  //// String input = task.getInputs().get("value");
  //// String output = task.getInputs().get("output");
  ////
  //// List<KeyValuePair> outputProperties = taskEntity.getOutputProperties();
  ////
  //// KeyValuePair outputProperty = new KeyValuePair();
  //// outputProperty.setKey(output);
  ////
  //// ControllerRequestProperties requestProperties = propertyManager
  //// .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
  //// String outputValue =
  //// propertyManager.replaceValueWithProperty(input, activity.getId(), requestProperties);
  ////
  //// outputProperty.setValue(outputValue);
  //// outputProperties.add(outputProperty);
  //// taskEntity.setOutputProperties(outputProperties);
  //// taskActivityService.save(taskEntity);
  ////
  //// }
  //

  // private void updatePendingAprovalStatus(ActivityEntity workflowActivity) {
  // long count = approvalService.getApprovalCountForActivity(workflowActivity.getId(),
  // ApprovalStatus.submitted);
  // boolean existingApprovals = (count > 0);
  // workflowActivity.setAwaitingApproval(existingApprovals);
  // this.activityService.saveWorkflowActivity(workflowActivity);
  // }


  private String getLock(String storeId, List<String> keys, long timeout) {
    RetryTemplate retryTemplate = getRetryTemplate();
    return retryTemplate.execute(ctx -> {
      final String token = lock.acquire(keys, "flow_task_locks", timeout);
      if (!StringUtils.isEmpty(token)) {
        throw new LockNotAvailableException(
            String.format("Lock not available for keys: %s in store %s", keys, storeId));
      }
      return token;
    });
  }

  private RetryTemplate getRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(2000l);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(100);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

  private void finishWorkflow(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks) {
    Optional<WorkflowEntity> workflow = workflowRepository.findById(wfRunEntity.getWorkflowRef());

    // TODO: implement Workflow Termination Endpoint
    // this.controllerClient.terminateFlow(workflow.getId(), workflow.getName(),
    // wfRunEntity.getId());

    //Loop through and validate all paths have been taken
    //It also updates the status of each task and checks dependencies.
    tasks.stream().filter(t -> TaskType.end.equals(t.getType())).forEach(t -> {
      t.setStatus(RunStatus.succeeded);
      t.setPhase(RunPhase.completed);
      taskRunRepository.save(t);
    });
    boolean workflowCompleted = dagUtility.validateWorkflow(wfRunEntity, tasks);

    if (wfRunEntity.getStatusOverride() != null) {
      wfRunEntity.setStatus(wfRunEntity.getStatusOverride());
    } else {
      if (workflowCompleted) {
        wfRunEntity.setStatus(RunStatus.succeeded);
      } else {
        wfRunEntity.setStatus(RunStatus.failed);
      }
    }
    wfRunEntity.setPhase(RunPhase.completed);

    long duration = new Date().getTime() - wfRunEntity.getStartTime().getTime();
    wfRunEntity.setDuration(duration);

    List<TaskRunEntity> taskRuns = taskRunRepository.findByWorkflowRunRef(wfRunEntity.getId());
    for (TaskRunEntity tr : taskRuns) {
      if (tr.getWorkflowResults() != null) {
        wfRunEntity.getResults().addAll(tr.getWorkflowResults());
      }
    }

    this.workflowRunRepository.save(wfRunEntity);
    LOGGER.info("[{}] Completed Workflow with status: {}.", wfRunEntity.getId(), wfRunEntity.getStatus());
  }

  private void executeNextStep(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks,
      TaskRunEntity currentTask, boolean finishedAll) {
    List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasks, currentTask);
    LOGGER.debug("[{}] Looking at next tasks. Found {} tasks. Tasks: {}", wfRunEntity.getId(),
        nextNodes.size(), nextNodes.toString());

    for (TaskRunEntity next : nextNodes) {
      if (TaskType.end.equals(next.getType())) {
        if (finishedAll) {
          LOGGER.debug("FINISHED ALL");
          this.finishWorkflow(wfRunEntity, tasks);
          return;
        }
        continue;
      }

      boolean executeTask = canExecuteTask(wfRunEntity, next);
      LOGGER.debug("[{}] Task: {}", wfRunEntity.getId(), next.getName());

      if (executeTask) {
        Optional<TaskRunEntity> taskRunEntity =
            this.taskRunRepository.findById(currentTask.getId());
        if (!taskRunEntity.isPresent()) {
          LOGGER.error("Reached node which should not be executed.");
        } else {
          flowClient.queueTask(this, next);
        }
      }
    }
  }

  private boolean finishedAll(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks,
      TaskRunEntity currentTask) {
    boolean finishedAll = true;
    List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasks, currentTask);
    LOGGER.debug("[{}] Task Dependencies: {}", currentTask.getId(), nextNodes.toString());
    for (TaskRunEntity next : nextNodes) {
      if (TaskType.end.equals(next.getType())) {
        List<TaskDependency> deps = next.getDependencies();
        for (TaskDependency dep : deps) {
          Optional<TaskRunEntity> taskRunEntity = this.taskRunRepository
              .findFirstByNameAndWorkflowRunRef(dep.getTaskRef(), wfRunEntity.getId());
          if (!taskRunEntity.isPresent() && taskRunEntity != null) {
            continue;
          }

          RunPhase phase = taskRunEntity.get().getPhase();
          if (!RunPhase.completed.equals(phase)) {
            finishedAll = false;
          }
        }
      }
    }

    return finishedAll;
  }

  private boolean canExecuteTask(WorkflowRunEntity wfRunEntity, TaskRunEntity next) {
    List<TaskDependency> deps = next.getDependencies();
    for (TaskDependency dep : deps) {
      Optional<TaskRunEntity> taskRunEntity = this.taskRunRepository.findById(dep.getTaskRef());
      if (taskRunEntity.isPresent()) {
        RunPhase phase = taskRunEntity.get().getPhase();
        if (!RunPhase.completed.equals(phase)) {
          return false;
        }
      }
    }
    return true;
  }

  // private Task getTask(TaskExecutionEntity taskActivity) {
  // ActivityEntity activity =
  // activityService.findWorkflowActivtyById(taskActivity.getActivityId());
  // RevisionEntity revision =
  // workflowVersionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
  // List<Task> tasks = createTaskList(revision, activity);
  // String taskId = taskActivity.getTaskId();
  // return tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
  // }

  private void updateStatusAndSaveTask(TaskRunEntity taskExecution, RunStatus status,
      RunPhase phase, Optional<String> message, Object... messageArgs) {
    if (RunStatus.failed.equals(status) && message.isPresent()) {
      LOGGER.error(MessageFormatter.arrayFormat(message.get(), messageArgs).getMessage());
    } else if (message.isPresent()) {
      taskExecution
          .setStatusMessage(MessageFormatter.arrayFormat(message.get(), messageArgs).getMessage());
    }
    taskExecution.setStatus(status);
    taskExecution.setPhase(phase);
    taskRunRepository.save(taskExecution);
  }
}
