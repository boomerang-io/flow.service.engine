package io.boomerang.service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.client.WorkflowClient;
import io.boomerang.data.entity.ActionEntity;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.ActionRepository;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.RunError;
import io.boomerang.model.RunParam;
import io.boomerang.model.RunResult;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.TaskWorkspace;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunSubmitRequest;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowWorkspaceSpec;
import io.boomerang.model.enums.ActionStatus;
import io.boomerang.model.enums.ActionType;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.model.enums.WorkflowScheduleType;
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
  
   @Autowired
   private WorkflowClient workflowClient;

  @Autowired
  private ParameterManager paramManager;

  @Autowired
  private TaskExecutionClient taskExecutionClient;

  @Autowired
  @Lazy
  @Qualifier("asyncTaskExecutor")
  TaskExecutor asyncTaskExecutor;

  @Override
  @Async("asyncTaskExecutor")
  public void queue(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.info("[{}] Recieved queue task request.", taskExecutionId);

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
      paramManager.resolveParamLayers(wfRunEntity.get(), Optional.of(taskExecution));

      // Update Status and Phase
      updateStatusAndSaveTask(taskExecution, RunStatus.ready, RunPhase.pending, Optional.empty());
    } else {
      LOGGER.info("[{}] Skipping task: {}", taskExecutionId, taskExecution.getName());
      taskExecution.setStatus(RunStatus.skipped);
      taskExecutionClient.end(this, taskExecution);
    }

    // Auto start System related tasks skipping the start checks
    if (!TaskType.template.equals(taskExecution.getType())
        && !TaskType.script.equals(taskExecution.getType())
        && !TaskType.custom.equals(taskExecution.getType())
        && !TaskType.generic.equals(taskExecution.getType())) {
      taskExecutionClient.execute(this, taskExecution, wfRunEntity.get());
    }
  }

  /*
   * Execute the Start of a task as requested by the Handler or System
   * 
   * This needs to get a lock on the TaskRun so that if a Handler makes additional requests on this
   * task, it waits for the end.
   * 
   * Note: This is synchronous such that a task is moved into the correct status before a response
   * is provided as part of the API. If the API returned immediately before these checks occur, an
   * integration may believe the task has started and therefore be able to run and end the task
   * prior to an asynchronous version of this method actually completing.
   */
  @Override
  public void start(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.info("[{}] Recieved start task request.", taskExecutionId);

    // Check if TaskRun Phase is valid. Pending means it correctly came from queueTask();
    if (!RunPhase.pending.equals(taskExecution.getPhase())) {
      LOGGER.debug("[{}] Task Status invalid.", taskExecutionId);
      return;
    }

    LOGGER.info("[{}] Attempting to acquire TaskRun ({}) lock", taskExecutionId, taskExecutionId);
    String taskTokenId = lockManager.acquireRunLock(taskExecutionId);
    LOGGER.info("[{}] Obtained TaskRun ({}) lock", taskExecutionId, taskExecutionId);

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find WorkflowRun"));

      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
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

      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
      return;
    } else if (hasWorkflowRunExceededTimeout(wfRunEntity.get())) {
      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
      // Checking WorkflowRun Timeout
      // Check prior to starting the TaskRun before further execution can happen
      // Timeout will mark the task as skipped.
      workflowRunService.timeout(wfRunEntity.get().getId(), false);
      return;
    }

    // LOGGER.info("[{}] Attempting to get WorkflowRun ({}) lock", taskExecutionId,
    // wfRunEntity.get().getId());
    // String tokenId = lockManager.acquireRunLock(wfRunEntity.get().getId());
    // LOGGER.info("[{}] Obtained WorkflowRun ({}) lock", taskExecutionId,
    // wfRunEntity.get().getId());

    // Ensure Task is valid as part of Graph
    // TODO: can we remove this expensive check considering it is in Queue?
    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get());
    boolean canRunTask = dagUtility.canCompleteTask(tasks, taskExecution);
    LOGGER.debug("[{}] Can run task? {}", taskExecutionId, canRunTask);

    // lockManager.releaseRunLock(wfRunEntity.get().getId(), tokenId);
    // LOGGER.info("[{}] Released WorkflowRun ({}) lock", taskExecutionId,
    // wfRunEntity.get().getId());

    lockManager.releaseRunLock(taskExecutionId, taskTokenId);
    LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);

    taskExecutionClient.execute(this, taskExecution, wfRunEntity.get());
  }

  /*
   * Execute the System tasks called from queue or start asynchronously
   * 
   * No status checks are performed as part of this method. They are handled in start().
   */
  @Override
  @Async("asyncTaskExecutor")
  public void execute(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.info("[{}] Recieved Execute task request.", taskExecutionId);

    // Execute based on TaskType
    TaskType taskType = taskExecution.getType();
    String wfRunId = wfRunEntity.getId();
    LOGGER.debug("[{}] Examining task type: {}", taskExecutionId, taskType);
    boolean callEnd = false;
    // Set up task
    taskExecution.setStartTime(new Date());
    updateStatusAndSaveTask(taskExecution, RunStatus.running, RunPhase.running, Optional.empty());
    /*
     * If new TaskTypes are added, the following code needs updated as well as the IF statement at
     * the end of QUEUE
     * 
     * TODO: migrate to CASE statement
     */
    if (TaskType.decision.equals(taskType)) {
      LOGGER.info("[{}] Execute Decision Task", wfRunId);
      processDecision(taskExecution, wfRunId);
      taskExecution.setStatus(RunStatus.succeeded);
      callEnd = true;
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
      String token = lockManager.acquireTaskLock(taskExecution, wfRunEntity.getId());
      if (token != null) {
        taskExecution.setStatus(RunStatus.succeeded);
      } else {
        taskExecution.setStatus(RunStatus.failed);
      }
    } else if (TaskType.releaselock.equals(taskType)) {
      LOGGER.info("[{}] Execute Release Lock", wfRunId);
      lockManager.releaseTaskLock(taskExecution, wfRunEntity.getId());
      taskExecution.setStatus(RunStatus.succeeded);
      callEnd = true;
    } else if (TaskType.runworkflow.equals(taskType)) {
      LOGGER.info("[{}] Execute Run Workflow Task", wfRunId);
      this.runWorkflow(taskExecution, wfRunEntity);
      callEnd = true;
    } else if (TaskType.runscheduledworkflow.equals(taskType)) {
      LOGGER.info("[{}] Execute Run Scheduled Workflow Task", wfRunId);
      this.runScheduledWorkflow(taskExecution, wfRunEntity);
      callEnd = true;
    } else if (TaskType.setwfstatus.equals(taskType)) {
      LOGGER.info("[{}] Save Workflow Status", wfRunId);
      saveWorkflowStatus(taskExecution, wfRunEntity);
      taskExecution.setStatus(RunStatus.succeeded);
      callEnd = true;
    } else if (TaskType.setwfproperty.equals(taskType)) {
      LOGGER.info("[{}] Execute Set Workflow Result Parameter Task", wfRunId);
      saveWorkflowProperty(taskExecution, wfRunEntity);
      taskExecution.setStatus(RunStatus.succeeded);
      callEnd = true;
    } else if (TaskType.approval.equals(taskType)) {
      LOGGER.info("[{}] Execute Approval Action Task", wfRunId);
      createActionTask(taskExecution, wfRunEntity, ActionType.approval);
    } else if (TaskType.manual.equals(taskType)) {
      LOGGER.info("[{}] Execute Manual Action Task", wfRunId);
      createActionTask(taskExecution, wfRunEntity, ActionType.manual);
    } else if (TaskType.eventwait.equals(taskType)) {
      LOGGER.info("[{}] Execute Wait For Event Task", wfRunId);
      createWaitForEventTask(taskExecution, callEnd);
    } else if (TaskType.sleep.equals(taskType)) {
      LOGGER.info("[{}] Execute Sleep Task", wfRunId);
      createSleepTask(taskExecution);
      callEnd = true;
    }

    // Check if task has a timeout set
    // If set, create Timeout Delayed CompletableFuture
    // TODO migrate to a scheduled task rather than using Future so that it works across horizontal
    // scaling
    if (!Objects.isNull(taskExecution.getTimeout()) && taskExecution.getTimeout() != 0) {
      LOGGER.debug("[{}] TaskRun Timeout provided of {} minutes. Creating future timeout check.",
          taskExecution.getId(), taskExecution.getTimeout());
      CompletableFuture.supplyAsync(timeoutTaskAsync(taskExecution.getId()), CompletableFuture
          .delayedExecutor(taskExecution.getTimeout(), TimeUnit.MINUTES, asyncTaskExecutor));
    }

    if (callEnd) {
      taskExecutionClient.end(this, taskExecution);
    }
  }

  /*
   * Execute the End of a task as requested by the Handler
   * 
   * This needs to get a lock on the TaskRun so that if a Handler requests end, it waits for the
   * start to finish.
   */
  @Override
  @Async("asyncTaskExecutor")
  public void end(TaskRunEntity taskExecution) {
    String taskExecutionId = taskExecution.getId();
    LOGGER.info("[{}] Recieved end task request.", taskExecutionId);

    // Check if task has been previously completed or cancelled
    if (RunPhase.completed.equals(taskExecution.getPhase())) {
      LOGGER.error("[{}] Task has already been completed or cancelled.", taskExecutionId);
      return;
    }

    LOGGER.info("[{}] Attempting to acquire TaskRun ({}) lock", taskExecutionId, taskExecutionId);
    String taskTokenId = lockManager.acquireRunLock(taskExecutionId);
    LOGGER.info("[{}] Obtained TaskRun ({}) lock", taskExecutionId, taskExecutionId);

    // Check if WorkflowRun Phase is valid
    Optional<WorkflowRunEntity> wfRunEntity =
        workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    if (!wfRunEntity.isPresent()) {
      updateStatusAndSaveTask(taskExecution, RunStatus.cancelled, RunPhase.completed,
          Optional.of("Unable to find WorkflowRun"));

      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
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

      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
      return;
    } else {
      // Update TaskRun with current TaskExecution status
      // updateStatusAndSaveTask is not used as we leave the Status to what user provided
      LOGGER.info("[{}] Marking Task as {}.", taskExecutionId, taskExecution.getStatus());
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      taskExecution.setPhase(RunPhase.completed);
      taskExecution = taskRunRepository.save(taskExecution);

      lockManager.releaseRunLock(taskExecutionId, taskTokenId);
      LOGGER.info("[{}] Released TaskRun ({}) lock", taskExecutionId, taskExecutionId);
    }

    // Execute Next Task (checking timeouts)
    // Check happens after saving the TaskRun to ensure we correctly record the provided user
    // details but no further execution can happen
    if (hasWorkflowRunExceededTimeout(wfRunEntity.get())) {
      // This will update the Workflow status and then call back to this method to be trapped by
      // above WorkflowRun checks
      workflowRunService.timeout(wfRunEntity.get().getId(), false);
      return;
    } else if (RunStatus.timedout.equals(taskExecution.getStatus())
        || hasTaskRunExceededTimeout(taskExecution)) {
      long duration = taskExecution.getStartTime() != null
          ? new Date().getTime() - taskExecution.getStartTime().getTime()
          : 0;
      taskExecution.setDuration(duration);
      updateStatusAndSaveTask(taskExecution, RunStatus.timedout, RunPhase.completed,
          Optional.of("The TaskRun exceeded the timeout. Timeout was set to {} minutes"),
          taskExecution.getTimeout());
      workflowRunService.timeout(wfRunEntity.get().getId(), true);
      return;
    }

    LOGGER.info("[{}] Attempting to get WorkflowRun ({}) lock", taskExecutionId,
        wfRunEntity.get().getId());
    String tokenId = lockManager.acquireRunLock(wfRunEntity.get().getId());
    LOGGER.info("[{}] Obtained WorkflowRun ({}) lock", taskExecutionId, wfRunEntity.get().getId());

    Optional<WorkflowRevisionEntity> wfRevisionEntity =
        workflowRevisionRepository.findById(wfRunEntity.get().getWorkflowRevisionRef());
    List<TaskRunEntity> tasks =
        dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity.get());
    boolean finishedAllDependencies = this.finishedAll(wfRunEntity.get(), tasks, taskExecution);
    LOGGER.debug("[{}] Finished all TaskRuns? {}", taskExecutionId, finishedAllDependencies);

    // Refresh wfRunEntity and update approval status
    wfRunEntity = workflowRunRepository.findById(taskExecution.getWorkflowRunRef());
    updatePendingAprovalStatus(wfRunEntity.get());

    executeNextStep(wfRunEntity.get(), tasks, taskExecution, finishedAllDependencies);

    lockManager.releaseRunLock(wfRunEntity.get().getId(), tokenId);
    LOGGER.info("[{}] Released WorkflowRun ({}) lock", taskExecutionId, wfRunEntity.get().getId());
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

      end(taskRunEntity.get());
    }
  }

  /*
   * An async method to execute Timeout checks with DelayedExecutor
   * 
   * The CompletableFuture.orTimeout() method can't be used as the TaskRun Async thread will finish
   * and hand over to the Handler and wait for callback.
   * 
   * TODO: save error block TODO: implement via quartz Note: Implements same locks as
   * TaskExecutionService
   */
  private Supplier<Boolean> timeoutTaskAsync(String taskRunId) {
    return () -> {
      final Optional<TaskRunEntity> optTaskExecution = this.taskRunRepository.findById(taskRunId);
      if (optTaskExecution.isPresent()) {
        TaskRunEntity taskExecution = optTaskExecution.get();
        // Only need to check if Running - otherwise nothing to timeout
        if (RunPhase.running.equals(taskExecution.getPhase())) {
          LOGGER.info("[{}] Timeout Task Async...", taskRunId);
          taskExecution.setStatus(RunStatus.timedout);
          taskExecutionClient.end(this, taskExecution);
        }
      }
      return true;
    };
  }

  private void getTaskWorkspaces(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    ObjectMapper mapper = new ObjectMapper();
    List<TaskWorkspace> taskWorkspaces = new LinkedList<>();
    wfRunEntity.getWorkspaces().forEach(ws -> {
      TaskWorkspace tw = new TaskWorkspace();
      WorkflowWorkspaceSpec spec = mapper.convertValue(ws.getSpec(), WorkflowWorkspaceSpec.class);
      tw.setName(ws.getName());
      tw.setMountPath(spec.getMountPath());
      tw.setOptional(ws.isOptional());
      tw.setType(ws.getType());
    });
    taskExecution.setWorkspaces(taskWorkspaces);
  }

  private void updatePendingAprovalStatus(WorkflowRunEntity wfRunEntity) {
    long count = actionRepository.countByWorkflowRunRefAndStatus(wfRunEntity.getId(),
        ActionStatus.submitted);
    boolean existingApprovals = (count > 0);
    wfRunEntity.setAwaitingApproval(existingApprovals);
    this.workflowRunRepository.save(wfRunEntity);
  }

  private boolean hasWorkflowRunExceededTimeout(WorkflowRunEntity wfRunEntity) {
    if (!Objects.isNull(wfRunEntity.getTimeout()) && wfRunEntity.getTimeout() != 0) {
      long duration = new Date().getTime() - wfRunEntity.getStartTime().getTime();
      long timeout = TimeUnit.MINUTES.toMillis(wfRunEntity.getTimeout());
      if (duration >= timeout) {
        return true;
      }
    }
    return false;
  }

  private boolean hasTaskRunExceededTimeout(TaskRunEntity taskRunEntity) {
    if (!Objects.isNull(taskRunEntity.getTimeout()) && taskRunEntity.getTimeout() != 0) {
      long duration = new Date().getTime() - taskRunEntity.getStartTime().getTime();
      long timeout = TimeUnit.MINUTES.toMillis(taskRunEntity.getTimeout());
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

  private void saveWorkflowStatus(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    String status = ParameterUtil.getValue(taskExecution.getParams(), "status").toString();
    if (!status.isBlank()) {
      RunStatus taskStatus = RunStatus.valueOf(status);
      wfRunEntity.setStatusOverride(taskStatus);
      this.workflowRunRepository.save(wfRunEntity);
    }
  }

  private void createSleepTask(TaskRunEntity taskExecution) {
    String value = ParameterUtil.getValue(taskExecution.getParams(), "duration").toString();
    long duration = Long.parseLong(value);

    try {
      Thread.sleep(duration);
      taskExecution.setStatus(RunStatus.succeeded);
    } catch (InterruptedException e) {
      taskExecution.setStatus(RunStatus.failed);
      RunError error = new RunError();
      error.setMessage(e.getMessage());
      taskExecution.setError(error);
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
      String workflowId =
          ParameterUtil.getValue(taskExecution.getParams(), "workflowId").toString();
      List<RunParam> wfRunParamsRequest =
          ParameterUtil.removeEntry(taskExecution.getParams(), "workflowId");
      if (workflowId != null) {
        // TODO: need to add the ability to set Trigger
        WorkflowRunSubmitRequest request = new WorkflowRunSubmitRequest();
        request.setTrigger("WorkflowRun");
        request.setParams(wfRunParamsRequest);
        try {
          request.setWorkflowRef(workflowId);
          WorkflowRun wfRunResponse = workflowRunService
              .submit(request, false).getBody();
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

  private void runScheduledWorkflow(TaskRunEntity taskExecution, WorkflowRunEntity wfRunEntity) {
    if (taskExecution.getParams() != null) {
       String workflowId = ParameterUtil.getValue(taskExecution.getParams(), "workflowId").toString();
       Integer futureIn = Integer.valueOf(ParameterUtil.getValue(taskExecution.getParams(), "futureIn").toString());
       String futurePeriod = ParameterUtil.getValue(taskExecution.getParams(), "futurePeriod").toString();
       String timezone = ParameterUtil.getValue(taskExecution.getParams(), "timezone").toString();
       String time = ParameterUtil.getValue(taskExecution.getParams(), "time").toString();
       Date executionDate = taskExecution.getCreationDate();
       LOGGER.debug("*******Run Scheduled Workflow System Task******");
       LOGGER.debug("Scheduling new task in " + futureIn + " " + futurePeriod);
       
       if (Objects.nonNull(futureIn) && futureIn != 0 && StringUtils.indexOfAny(futurePeriod,
       new String[] {"minutes", "hours", "days", "weeks", "months"}) >= 0) {
         Calendar executionCal = Calendar.getInstance();
         executionCal.setTime(executionDate);
         Integer calField = Calendar.MINUTE;
         switch (futurePeriod) {
           case "hours":
           calField = Calendar.HOUR;
           break;
           case "days":
           calField = Calendar.DATE;
           break;
           case "weeks":
           futureIn = futureIn * 7;
           calField = Calendar.DATE;
           break;
           case "months":
           calField = Calendar.MONTH;
           break;
         }
         executionCal.add(calField, futureIn);
       
         if (!futurePeriod.equals("minutes") && !futurePeriod.equals("hours")) {
           String[] hoursTime = time.split(":");
           Integer hours = Integer.valueOf(hoursTime[0]);
           Integer minutes = Integer.valueOf(hoursTime[1]);
           LOGGER
           .debug("With time to be set to: " + time + " in " + timezone);
           executionCal.setTimeZone(TimeZone.getTimeZone(timezone));
           executionCal.set(Calendar.HOUR, hours);
           executionCal.set(Calendar.MINUTE, minutes);
           LOGGER.debug(
           "With execution set to: " + executionCal.getTime().toString() + " in " + timezone);
           executionCal.setTimeZone(TimeZone.getTimeZone("UTC"));
         }
         LOGGER.debug("With execution set to: " + executionCal.getTime().toString() + " in UTC");
      
         // Define new properties removing the System Task specific properties
         // TODO - determine if we need to resolve any param layers before executing new workflow
         List<RunParam> newParamList = taskExecution.getParams();
         ParameterUtil.removeEntry(newParamList, "workflowId");
         ParameterUtil.removeEntry(newParamList, "futureIn");
         ParameterUtil.removeEntry(newParamList, "futurePeriod");
         ParameterUtil.removeEntry(newParamList, "timezone");
         ParameterUtil.removeEntry(newParamList, "time");
       
         // Define and create the schedule
         WorkflowSchedule schedule = new WorkflowSchedule();
         schedule.setWorkflowRef(workflowId);
         schedule.setName(taskExecution.getName());
         schedule
         .setDescription("This schedule was generated through a Run Scheduled Workflow task.");
         schedule.setParams(newParamList);
         schedule.setDateSchedule(executionCal.getTime());
         schedule.setTimezone(timezone);
         schedule.setType(WorkflowScheduleType.runOnce);
         WorkflowSchedule workflowSchedule = workflowClient.createSchedule(schedule);
         if (workflowSchedule != null && workflowSchedule.getId() != null) {
           LOGGER.debug("Workflow Scheudle (" + workflowSchedule.getId() + ") created.");
           taskExecution.setStatus(RunStatus.succeeded);
           return;
         }
       }
    } 
    taskExecution.setStatus(RunStatus.failed);
  }

  private void createWaitForEventTask(TaskRunEntity taskExecution, boolean callEnd) {
    LOGGER.debug("[{}] Creating wait for event task", taskExecution.getId());
    taskExecution.setStatus(RunStatus.waiting);
    taskExecution = taskRunRepository.save(taskExecution);

    if (taskExecution.isPreApproved()) {
      taskExecution.setStatus(RunStatus.succeeded);
      taskExecution = taskRunRepository.save(taskExecution);
      callEnd = true;
    }
  }

  /*
   * Creates an Action entity of Manual or Approval type
   * 
   * If of Approval type, will check for optional number of approvers and approverGroup
   */
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

    if (taskExecution.getParams() != null) {
      if (type.equals(ActionType.approval)) {
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
      } else if (type.equals(ActionType.manual)) {
        if (ParameterUtil.containsName(taskExecution.getParams(), "instructions")) {
          String instructions =
              (String) ParameterUtil.getValue(taskExecution.getParams(), "instructions");
          if (instructions != null && !instructions.isBlank()) {
            actionEntity.setInstructions(instructions);
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

    String tokenId = lockManager.acquireRunLock(wfRunEntity.getId());

    List<RunResult> wfResults = wfRunEntity.getResults();
    RunResult wfResult = new RunResult();
    wfResult.setName(output);
    wfResult.setValue(input);
    wfResults.add(wfResult);
    wfRunEntity.setResults(wfResults);
    workflowRunRepository.save(wfRunEntity);

    lockManager.releaseRunLock(wfRunEntity.getId(), tokenId);
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
      if (executeTask) {
        LOGGER.debug("[{}] Execute next TaskRun: {}", wfRunEntity.getId(), next.getName());
        Optional<TaskRunEntity> taskRunEntity =
            this.taskRunRepository.findById(currentTask.getId());
        if (!taskRunEntity.isPresent()) {
          LOGGER.error("Reached node which should not be executed.");
        } else {
          taskExecutionClient.queue(this, next);
        }
      } else {
        LOGGER.debug(
            "[{}] Unable to execute next TaskRun: {}. Not all dependencies have been completed.",
            wfRunEntity.getId(), next.getName());
      }
    }
  }

  /*
   * Checks if all the dependencies for the End task have been completed
   */
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
    LOGGER.debug("Found {} dependencies", deps.size());
    for (TaskDependency dep : deps) {
      Optional<TaskRunEntity> taskRunEntity = this.taskRunRepository
          .findFirstByNameAndWorkflowRunRef(dep.getTaskRef(), wfRunEntity.getId());
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
