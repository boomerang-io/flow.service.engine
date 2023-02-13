package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.ActionEntity;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.ActionRepository;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.RunParam;
import io.boomerang.model.RunResult;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.TaskWorkspace;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.WorkflowWorkspaceSpec;
import io.boomerang.model.enums.ActionStatus;
import io.boomerang.model.enums.ActionType;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.util.ParameterUtil;

@Service
public class TaskExecutionServiceImpl implements TaskExecutionService {

  private static final Logger LOGGER = LogManager.getLogger(TaskExecutionServiceImpl.class);

  @Autowired
  @Lazy
  private LockManager lockManager;

  @Autowired
  private DAGUtility dagUtility;

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private WorkflowRunService workflowRunService;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private ActionRepository actionRepository;
  //
  // @Autowired
  // private WorkflowScheduleService scheduleService;

  @Autowired
  private ParameterManager paramManager;

  @Override
  @Async("asyncTaskExecutor")
  public void queueTask(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.debug("[{}] Recieved queue task request.", taskExecutionId);

    // Check if TaskRun Phase is valid
    if (!RunPhase.pending.equals(taskExecution.getPhase())) {
      LOGGER.error("[{}] Task Status invalid. Cannot queue task.", taskExecutionId);
      return;
    }

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find WorkflowRun"));
      return;
    } else if (RunPhase.completed.equals(wfRunEntity.get().getPhase())
        || RunPhase.finalized.equals(wfRunEntity.get().getPhase())) {
      // Set duration. If in Queued. There will be no start time.
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      updateStatusAndSaveTask(taskExecution, RunStatus.skipped, RunPhase.completed, Optional.of(
          "[{}] WorkflowRun has been marked as {}. TaskRun was never queued, setting TaskRun as Skipped."),
          wfRunEntity.get().getId(), wfRunEntity.get().getStatus());
      return;
    }

    // Ensure Task is valid as part of Graph
    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get());
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);
    LOGGER.debug("[{}] Can run task? {}", taskExecutionId, canRunTask);

    if (canRunTask) {
      // Resolve Parameter Substitutions
      paramManager.resolveTaskRunParams(wfRunEntity.get().getId(), wfRunEntity.get().getParams(),
          taskExecution.getParams());

      // Update Status and Phase
      updateStatusAndSaveTask(taskExecution, RunStatus.ready, RunPhase.pending, Optional.empty());
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

    // Check if TaskRun Phase is valid. Pending means it correctly came from queueTask();
    if (!RunPhase.pending.equals(taskExecution.getPhase())) {
      LOGGER.debug("[{}] Task Status invalid.", taskExecutionId);
      return;
    }

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find WorkflowRun"));
      return;
    } else if (RunPhase.completed.equals(wfRunEntity.get().getPhase())
        || RunPhase.finalized.equals(wfRunEntity.get().getPhase())) {
      // Set duration. If in Queued. There will be no start time.
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed, Optional.of(
          "[{}] WorkflowRun has been marked as {}. Setting TaskRun as Cancelled. TaskRun may still run to completion."),
          wfRunEntity.get().getId(), wfRunEntity.get().getStatus());
      return;
    } else if (hasExceededTimeout(wfRunEntity.get())) {
      // Checking WorkflowRun Timeout
      // Check prior to starting the TaskRun before further execution can happen
      // Timeout will mark the task as skipped.
      workflowRunService.timeout(wfRunEntity.get().getId());
      return;
    }

    // Ensure Task is valid as part of Graph
    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get());
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);
    LOGGER.debug("[{}] Can run task? {}", taskExecutionId, canRunTask);

    // Execute based on TaskType
    TaskType taskType = taskExecution.getType();
    String wfRunId = wfRunEntity.get().getId();
    LOGGER.debug("[{}] Examining task type: {}", taskExecutionId, taskType);
    if (canRunTask) {
      // Set up task
      taskExecution.setStartTime(new Date());
      updateStatusAndSaveTask(taskExecution, RunStatus.running, RunPhase.running, Optional.empty());
      if (TaskType.decision.equals(taskType)) {
        LOGGER.info("[{}] Execute Decision Task", wfRunId);
        processDecision(taskExecution, wfRunId);
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.template.equals(taskType) || TaskType.script.equals(taskType)) {
        LOGGER.info("[{}] Execute Template Task", wfRunId);
        getTaskWorkspaces(taskExecution, wfRunEntity);
      } else if (TaskType.custom.equals(taskType)) {
        LOGGER.info("[{}] Execute Custom Task", wfRunId);
        getTaskWorkspaces(taskExecution, wfRunEntity);
      } else if (TaskType.generic.equals(taskType)) {
        LOGGER.info("[{}] Execute Generic Task", wfRunId);
        // Nothing to do here. Generic task is completely up to the Handler.
        getTaskWorkspaces(taskExecution, wfRunEntity);
      } else if (TaskType.acquirelock.equals(taskType)) {
        LOGGER.info("[{}] Execute Acquire Lock", wfRunId);
        String token = lockManager.acquireTaskLock(taskExecution, wfRunEntity.get().getId());
        if (token != null) {
          taskExecution.setStatus(RunStatus.succeeded);
        } else {
          taskExecution.setStatus(RunStatus.failed);
        }
        this.endTask(taskExecution);
      } else if (TaskType.releaselock.equals(taskType)) {
        LOGGER.info("[{}] Execute Release Lock", wfRunId);
        lockManager.releaseTaskLock(taskExecution, wfRunEntity.get().getId());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.runworkflow.equals(taskType)) {
        LOGGER.info("[{}] Execute Run Workflow Task", wfRunId);
        this.runWorkflow(taskExecution, wfRunEntity.get());
        this.endTask(taskExecution);
      } else if (TaskType.runscheduledworkflow.equals(taskType)) {
        LOGGER.info("TODO - Run Scheduled Workflow");
        // TODO: this.runScheduledWorkflow(taskExecution, wfRunEntity.get(), workflowName);
      } else if (TaskType.setwfstatus.equals(taskType)) {
        LOGGER.info("[{}] Save Workflow Status", wfRunId);
        saveWorkflowStatus(taskExecution, wfRunEntity.get());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.setwfproperty.equals(taskType)) {
        LOGGER.info("[{}] Execute Set Workflow Result Parameter Task", wfRunId);
        saveWorkflowProperty(taskExecution, wfRunEntity.get());
        taskExecution.setStatus(RunStatus.succeeded);
        this.endTask(taskExecution);
      } else if (TaskType.approval.equals(taskType)) {
        LOGGER.info("[{}] Execute Approval Action Task", wfRunId);
        createActionTask(taskExecution, wfRunEntity.get(), ActionType.approval);
      } else if (TaskType.manual.equals(taskType)) {
        LOGGER.info("[{}] Execute Manual Action Task", wfRunId);
        createActionTask(taskExecution, wfRunEntity.get(), ActionType.manual);
      } else if (TaskType.eventwait.equals(taskType)) {
        LOGGER.info("TODO - Wait for Event");
        // TODO: createWaitForEventTask(taskExecution);
      }
    } else {
      LOGGER.debug("[{}] Skipping task: {}", taskExecutionId, taskExecution.getName());
      taskExecution.setStatus(RunStatus.skipped);
      endTask(taskExecution);
    }
  }

  private void getTaskWorkspaces(TaskRunEntity taskExecution,
      Optional<WorkflowRunEntity> wfRunEntity) {
    ObjectMapper mapper = new ObjectMapper();
    List<TaskWorkspace> taskWorkspaces = new LinkedList<>();
    wfRunEntity.get().getWorkspaces().forEach(ws -> {
      TaskWorkspace tw = new TaskWorkspace();
      WorkflowWorkspaceSpec spec = mapper.convertValue(ws.getSpec(), WorkflowWorkspaceSpec.class);
      tw.setName(ws.getName());
      tw.setMountPath(spec.getMountPath());
      tw.setOptional(ws.isOptional());
      tw.setType(ws.getType());
    });
    taskExecution.setWorkspaces(taskWorkspaces);
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

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find WorkflowRun"));
      return;
    } else if (RunPhase.completed.equals(wfRunEntity.get().getPhase())
        || RunPhase.finalized.equals(wfRunEntity.get().getPhase())) {
      // Set duration. If in Queued. There will be no start time.
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed, Optional.of(
          "[{}] WorkflowRun has been marked as {}. Setting TaskRun as Cancelled. TaskRun may still run to completion."),
          wfRunEntity.get().getId(), wfRunEntity.get().getStatus());
      // return;
    } else {
      // Update TaskRun with current TaskExecution status
      // updateStatusAndSaveTask is not used as we leave the Status to what user provided
      LOGGER.info("[{}] Marking Task as {}.", taskRunId, taskExecution.getStatus());
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      taskExecution.setPhase(RunPhase.completed);
      taskExecution = taskRunRepository.save(taskExecution);
    }

    // Execute Next Task (checking timeout)
    // Check happens after saving the TaskRun to ensure we correctly record the provided user
    // details but no further execution can happen
    if (hasExceededTimeout(wfRunEntity.get())) {
      workflowRunService.timeout(wfRunEntity.get().getId());
    } else {
      Optional<WorkflowRevisionEntity> wfRevisionEntity =
          workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
      List<TaskRunEntity> tasks =
          dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get());
      boolean finishedAllDependencies = this.finishedAll(wfRunEntity.get(), tasks, taskExecution);
      LOGGER.debug("[{}] Finished all previous tasks? {}", taskRunId, finishedAllDependencies);

      List<String> keys = new LinkedList<>();
      keys.add(wfRunEntity.get().getId());
      LOGGER.debug("[{}] Attempting to get WorkflowRun lock", taskRunId);
      String tokenId = lockManager.acquireWorkflowLock(keys);
      LOGGER.debug("[{}] Obtained WorkflowRun lock", taskRunId);
      // Refresh wfRunEntity and update approval status
      wfRunEntity = workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
      updatePendingAprovalStatus(wfRunEntity.get());

      executeNextStep(wfRunEntity.get(), tasks, taskExecution, finishedAllDependencies);

      lockManager.releaseWorkflowLock(keys, tokenId);
      LOGGER.debug("[{}] Released WorkflowRun lock", taskRunId);
    }
  }

  private void updatePendingAprovalStatus(WorkflowRunEntity wfRunEntity) {
    long count = actionRepository.countByWorkflowRunRefAndStatus(wfRunEntity.getId(),
        ActionStatus.submitted);
    boolean existingApprovals = (count > 0);
    wfRunEntity.setAwaitingApproval(existingApprovals);
    this.workflowRunRepository.save(wfRunEntity);
  }

  private boolean hasExceededTimeout(WorkflowRunEntity wfRunEntity) {
    if (!Objects.isNull(wfRunEntity.getTimeout()) && wfRunEntity.getTimeout() != -1
        && wfRunEntity.getTimeout() != 0) {
      long duration = new Date().getTime() - wfRunEntity.getStartTime().getTime();
      long timeout = TimeUnit.MINUTES.toMillis(wfRunEntity.getTimeout());
      if (duration >= timeout) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<String> updateTaskRunForTopic(String workflowRunId, String topic) {
    List<String> ids = new LinkedList<>();

    LOGGER.info("[{}] Finding taskRunId based on topic.", workflowRunId);
    List<TaskRunEntity> taskRunEntities =
        this.taskRunRepository.findByWorkflowRunRef(workflowRunId);

    for (TaskRunEntity taskRun : taskRunEntities) {
      if (TaskType.eventwait.equals(taskRun.getType())) {
        List<RunParam> params = taskRun.getParams();
        if (params != null && ParameterUtil.containsName(params, "topic")) {
          // TODO: bring back parameter layering
          // String paramTopic = params.get("topic").toString();
          // ControllerRequestProperties properties = propertyManager
          // .buildRequestPropertyLayering(null, taskRunId, activity.getWorkflowId());
          // topic = propertyManager.replaceValueWithProperty(paramTopic, taskRunId, properties);
          // String taskId = task.getId();
          LOGGER.info("[{}] Found task run id: {} ", workflowRunId, taskRun.getId());
          taskRun.setPreApproved(true);
          this.taskRunRepository.save(taskRun);
          ids.add(taskRun.getId());
        }
      }
    }

    // TODO: figure out what to return
    LOGGER.info("[{}] No task activity ids found for topic: {}", workflowRunId, topic);
    return ids;
  }

  // TODO: confirm if needed - currently not used.
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

  private void saveWorkflowStatus(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    String status = ParameterUtil.getValue(taskExecution.getParams(), "status").toString();
    if (!status.isBlank()) {
      RunStatus taskStatus = RunStatus.valueOf(status);
      wfRunEntity.setStatusOverride(taskStatus);
      this.workflowRunRepository.save(wfRunEntity);
    }
  }

  private void processDecision(TaskRunEntity taskExecution, String activityId) {
    String decisionValue = ParameterUtil.getValue(taskExecution.getParams(), "value").toString();
    // ControllerRequestProperties properties =
    // propertyManager.buildRequestPropertyLayering(taskExecution, activityId,
    // task.getWorkflowId());
    String value = decisionValue;
    // value = propertyManager.replaceValueWithProperty(value, activityId, properties);
    taskExecution.setDecisionValue(value);
    taskRunRepository.save(taskExecution);
  }

  private void runWorkflow(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    if (taskExecution.getParams() != null) {
      // TODO: need to add the ability to set Trigger
      Optional<WorkflowRunRequest> wfRunRequest = Optional.of(new WorkflowRunRequest());

      String workflowId =
          ParameterUtil.getValue(taskExecution.getParams(), "workflowId").toString();
      List<RunParam> wfRunParamsRequest =
          ParameterUtil.removeEntry(taskExecution.getParams(), "workflowId");
      wfRunRequest.get().setParams(wfRunParamsRequest);
      if (workflowId != null) {
        try {
          WorkflowRun wfRunResponse = workflowRunService
              .submit(workflowId, Optional.empty(), false, wfRunRequest).getBody();
          List<RunResult> wfRunResultResponse = new LinkedList<>();
          RunResult runResult = new RunResult();
          runResult.setName("workflowRunRef");
          runResult.setValue(wfRunResponse.getId());
          taskExecution.setResults(wfRunResultResponse);
          taskExecution.setStatus(RunStatus.succeeded);
        } catch (Exception e) {
          taskExecution.setStatus(RunStatus.failed);
        }
        taskRunRepository.save(taskExecution);
        return;
      }
    }
    taskExecution.setStatus(RunStatus.failed);
    taskRunRepository.save(taskExecution);
  }

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

  private void createActionTask(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity,
      ActionType type) {
    ActionEntity actionEntity = new ActionEntity();
    actionEntity.setTaskRunRef(taskExecution.getId());
    actionEntity.setWorkflowRunRef(wfRunEntity.getId());
    actionEntity.setWorkflowRef(wfRunEntity.getWorkflowRef());
    actionEntity.setStatus(ActionStatus.submitted);
    actionEntity.setType(type);
    actionEntity.setCreationDate(new Date());
    actionEntity.setNumberOfApprovers(1);

    if (type.equals(ActionType.approval)) {
      if (taskExecution.getParams() != null) {
        if (ParameterUtil.containsName(taskExecution.getParams(), "approverGroupId")) {
          String approverGroupId =
              (String) ParameterUtil.getValue(taskExecution.getParams(), "approverGroupId");
          if (approverGroupId != null && !approverGroupId.isBlank()) {
            actionEntity.setApproverGroupRef(approverGroupId);
          }
        }

        if (ParameterUtil.containsName(taskExecution.getParams(), "numberOfApprovers")) {
          String numberOfApprovers =
              (String) ParameterUtil.getValue(taskExecution.getParams(), "numberOfApprovers");
          if (numberOfApprovers != null && !numberOfApprovers.isBlank()) {
            actionEntity.setNumberOfApprovers(Integer.valueOf(numberOfApprovers));
          }
        }
      }
    }
    actionRepository.save(actionEntity);
    taskExecution.setStatus(RunStatus.waiting);
    taskExecution = taskRunRepository.save(taskExecution);
    wfRunEntity.setAwaitingApproval(true);
    this.workflowRunRepository.save(wfRunEntity);
  }


  // TODO: parameter layering
  private void saveWorkflowProperty(TaskRunEntity taskRunEntity, WorkflowRunEntity wfRunEntity) {
    String input = (String) taskRunEntity.getParams().stream()
        .filter(p -> "value".equals(p.getName())).findFirst().get().getValue();
    String output = (String) taskRunEntity.getParams().stream()
        .filter(p -> "output".equals(p.getName())).findFirst().get().getValue();

    // ControllerRequestProperties requestProperties = propertyManager
    // .buildRequestPropertyLayering(task, activity.getId(), activity.getWorkflowId());
    // String outputValue =
    // propertyManager.replaceValueWithProperty(input, activity.getId(), requestProperties);

    List<String> keys = new LinkedList<>();
    keys.add(wfRunEntity.getId());
    String tokenId = lockManager.acquireWorkflowLock(keys);

    List<RunResult> wfResults = wfRunEntity.getResults();
    RunResult wfResult = new RunResult();
    wfResult.setName(output);
    wfResult.setValue(input);
    wfResults.add(wfResult);
    wfRunEntity.setResults(wfResults);
    workflowRunRepository.save(wfRunEntity);

    lockManager.releaseWorkflowLock(keys, tokenId);
  }

  private void finishWorkflow(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks) {
    // Updates the status of end task
    tasks.stream().filter(t -> TaskType.end.equals(t.getType())).forEach(t -> {
      t.setStatus(RunStatus.succeeded);
      t.setPhase(RunPhase.completed);
      taskRunRepository.save(t);
    });
    // Validate all paths have been taken
    // It also updates the status of each task and checks dependencies.
    boolean workflowCompleted = dagUtility.validateWorkflow(wfRunEntity, tasks);

    // Set WorkflowRun status and phase
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

    // Calc Duration
    long duration = new Date().getTime() - wfRunEntity.getStartTime().getTime();
    wfRunEntity.setDuration(duration);

    this.workflowRunRepository.save(wfRunEntity);
    LOGGER.info("[{}] Completed Workflow with status: {}.", wfRunEntity.getId(),
        wfRunEntity.getStatus());
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
          this.queueTask(next);
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
            // Performance wise we don't need to finish the looping and can exit
            return finishedAll;
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

  public void updateStatusAndSaveTask(TaskRunEntity taskExecution, RunStatus status, RunPhase phase,
      Optional<String> message, Object... messageArgs) {
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
