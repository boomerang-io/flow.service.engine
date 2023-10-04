package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.util.GraphProcessor;

@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  private static final Logger LOGGER = LogManager.getLogger(WorkflowExecutionServiceImpl.class);

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private DAGUtility dagUtility;

  @Autowired
  @Lazy
  private TaskExecutionService taskService;

  @Autowired
  private TaskExecutionClient taskClient;

  @Autowired
  private ParameterManager paramManager;

  @Autowired
  @Lazy
  private LockManager lockManager;

  @Autowired
  @Lazy
  private WorkflowRunService workflowRunService; 
  
  @Autowired
  @Lazy
  @Qualifier("asyncWorkflowExecutor")
  TaskExecutor asyncWorkflowExecutor;       

  @Override
  public void queue(WorkflowRunEntity wfRunEntity) {
    LOGGER.debug("[{}] Recieved queue WorkflowRun request.", wfRunEntity.getId());
    // Resolve Parameter Substitutions
    // TODO: check if we need this
    paramManager.resolveParamLayers(wfRunEntity, Optional.empty());

    final Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity =
        this.workflowRevisionRepository.findById(wfRunEntity.getWorkflowRevisionRef());
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity wfRevisionEntity = optWorkflowRevisionEntity.get();
      final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, wfRunEntity);
      LOGGER.info("[{}] Found {} tasks: {}", wfRunEntity.getId(), tasks.size(), tasks.toString());
      if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
        updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.ready, RunPhase.pending,
            Optional.empty());
        return;
      }
    }
    updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.invalid, RunPhase.completed,
        Optional.of("Failed to run workflow: incomplete, or invalid, workflow"));
    throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION",
        "[{0}] Failed to run workflow: incomplete, or invalid, workflow",
        HttpStatus.INTERNAL_SERVER_ERROR, wfRunEntity.getId());
  }

  @Override
  public CompletableFuture<Boolean> start(WorkflowRunEntity wfRunEntity) {
    LOGGER.debug("[{}] Recieved start WorkflowRun request.", wfRunEntity.getId());
    //Check the WorkflowRun has been queued, throw if not
    //Don't update the WorkflowRun status as this may cause a running WorkflowRun to be incorrectly changed.
    if (!RunPhase.pending.equals(wfRunEntity.getPhase())) {
      throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_PHASE, wfRunEntity.getPhase(), RunPhase.pending);
    }
    final List<TaskRunEntity> tasks = dagUtility.retrieveTaskList(wfRunEntity.getId());
    final TaskRunEntity start = dagUtility.getTaskByType(tasks, TaskType.start);
    final TaskRunEntity end = dagUtility.getTaskByType(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
    if (!Objects.isNull(wfRunEntity.getTimeout())
        && wfRunEntity.getTimeout() != 0) {
      // Create Timeout Delayed CompletableFuture 
      LOGGER.debug("[{}] WorkflowRun Timeout provided of {} minutes. Creating future timeout check.", wfRunEntity.getId(), wfRunEntity.getTimeout());
      CompletableFuture.supplyAsync(timeoutWorkflowAsync(wfRunEntity.getId()),
          CompletableFuture.delayedExecutor(wfRunEntity.getTimeout(), TimeUnit.MINUTES, asyncWorkflowExecutor));
    }
    return CompletableFuture
        .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks), asyncWorkflowExecutor);
  }

  @Override
  public void end(WorkflowRunEntity workflowExecution) {
    workflowExecution.setPhase(RunPhase.finalized);
    workflowRunRepository.save(workflowExecution);
  }

  @Override
  public void cancel(WorkflowRunEntity workflowExecution) {
    workflowExecution.setStatus(RunStatus.cancelled);
    workflowExecution.setPhase(RunPhase.completed);
    workflowRunRepository.save(workflowExecution);
  }

  @Override
  public void timeout(WorkflowRunEntity workflowExecution) {
    if (RunStatus.timedout.equals(workflowExecution.getStatus()) || (!Objects.isNull(workflowExecution.getTimeout())
        && workflowExecution.getTimeout() != 0)) {
      CompletableFuture.supplyAsync(timeoutWorkflowAsync(workflowExecution.getId()));
    }
  }

  private Supplier<Boolean> executeWorkflowAsync(String wfRunId, final TaskRunEntity start,
      final TaskRunEntity end, final Graph<String, DefaultEdge> graph,
      final List<TaskRunEntity> tasksToRun) {
    return () -> {
      LOGGER.debug("[{}] Attempting to acquire WorkflowRun lock...", wfRunId);
      String lockId = lockManager.acquireLock(wfRunId);
      LOGGER.info("[{}] Obtained WorkflowRun lock",wfRunId);
      final Optional<WorkflowRunEntity> optWorkflowRunEntity =
          this.workflowRunRepository.findById(wfRunId);
      if (optWorkflowRunEntity.isPresent()) {
        WorkflowRunEntity wfRunEntity = optWorkflowRunEntity.get();
        //Check the WorkflowRun has been queued, throw if not
        //Don't update the WorkflowRun status as this may cause a running WorkflowRun to be incorrectly changed.
        if (!RunPhase.pending.equals(wfRunEntity.getPhase())) {
          lockManager.releaseLock(wfRunId, lockId);
          LOGGER.info("[{}] Released WorkflowRun lock", wfRunId);
          throw new BoomerangException(BoomerangError.WORKFLOWRUN_INVALID_PHASE, wfRunEntity.getPhase(), RunPhase.pending);
        }
        // Set Workflow to Running (Status and Phase). From this point, the duration needs to be
        // calculated.
        wfRunEntity.setStartTime(new Date());
        updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.running, RunPhase.running,
            Optional.empty());
        LOGGER.info("[{}] Executing Workflow Async...", wfRunEntity.getId());
        try {
          List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
          LOGGER.debug("[{}] Next Nodes Size: {}", wfRunEntity.getId(), nextNodes.size());
          for (TaskRunEntity next : nextNodes) {
            final List<String> nodes =
                GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

            if (nodes.contains(next.getId())) {
              LOGGER.debug("[{}] Creating TaskRun ({})...", wfRunEntity.getId(),
                  next.getId());
              taskClient.queue(taskService, next);
            }
          }
        } catch (Exception e) {
          updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.invalid, RunPhase.completed,
              Optional
                  .of("Failed to run workflow: unable to process Workflow and queue all tasks."));
          lockManager.releaseLock(wfRunId, lockId);
          LOGGER.info("[{}] Released WorkflowRun lock", wfRunId);
          throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION",
              "[{0}] Failed to run workflow: unable to process Workflow and queue all tasks",
              HttpStatus.INTERNAL_SERVER_ERROR, wfRunEntity.getId());
        }
      }
      lockManager.releaseLock(wfRunId, lockId);
      LOGGER.info("[{}] Released WorkflowRun lock", wfRunId);
      return true;
    };
  }

  /*
   * An async method to execute Timeout checks with DelayedExecutor
   * 
   * The CompletableFuture.orTimeout() method can't be used as the WorkflowRun async thread will
   * finish and hand over to TaskRun threads and thus never reaches timeout.
   * 
   * TODO: save error block
   * Note: Implements same locks as TaskExecutionService
   */
  private Supplier<Boolean> timeoutWorkflowAsync(String wfRunId) {
    return () -> {
      final Optional<WorkflowRunEntity> optWorkflowRunEntity =
          this.workflowRunRepository.findById(wfRunId);
      if (optWorkflowRunEntity.isPresent()) {
        WorkflowRunEntity wfRunEntity = optWorkflowRunEntity.get();
        // Only need to check if Workflow is running - otherwise nothing to timeout
        if (RunPhase.running.equals(wfRunEntity.getPhase())) {
          LOGGER.info("[{}] Timeout Workflow Async...", wfRunId);
          String tokenId = lockManager.acquireLock(wfRunId);
          LOGGER.debug("[{}] Obtained WorkflowRun lock", wfRunId);

          long duration = new Date().getTime() - wfRunEntity.getStartTime().getTime();
          wfRunEntity.setDuration(duration);
          String statusMessage = "The WorkflowRun exceeded the timeout. Timeout was set to {} minutes";
          if (wfRunEntity.getAnnotations().containsKey("boomerang.io/timeout-cause") && "TaskRun".equals(wfRunEntity.getAnnotations().get("boomerang.io/timeout-cause"))) {
            statusMessage = "A TaskRun exceeded it's timeout.";
          } else {
            wfRunEntity.getAnnotations().put("boomerang.io/timeout-cause", "WorkflowRun");
          }
          updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.timedout, RunPhase.completed,
              Optional.of(statusMessage),
              wfRunEntity.getTimeout());

          // Cancel Running Tasks
          Optional<WorkflowRevisionEntity> wfRevisionEntity =
              workflowRevisionRepository.findById(wfRunEntity.getWorkflowRevisionRef());
          List<TaskRunEntity> tasks =
              dagUtility.createTaskList(wfRevisionEntity.get(), wfRunEntity);

          // If running tasks are found, the TaskRun execution loop will automatically cancel in
          // flight tasks when you end them based on workflow status and skip all queued
          List<TaskRunEntity> runningTasks =
              tasks.stream().filter(t -> RunPhase.running.equals(t.getPhase())).toList();
          LOGGER.info("Timeout - # of Running Tasks: " + runningTasks.size());
          if (runningTasks.size() > 0) {
            runningTasks.forEach(t -> {
              taskClient.end(taskService, t);
            });
          } 
          // Check pending tasks and queue to force them to skip - will be
          // trapped by queue task before task order is checked
          List<TaskRunEntity> pendingTasks =
              tasks.stream().filter(t -> RunPhase.pending.equals(t.getPhase())).toList();
          LOGGER.info("Timeout - # of Pending Tasks: " + pendingTasks.size());
          if (pendingTasks.size() > 0) {
            pendingTasks.forEach(t -> {
              taskClient.queue(taskService, t);
            });
          }
          // Retry workflow and set required details
          if (!Objects.isNull(wfRunEntity.getRetries()) && wfRunEntity.getRetries() != -1
              && wfRunEntity.getRetries() != 0) {
            long retryCount = 0;
            if (wfRunEntity.getAnnotations().containsKey("boomerang.io/retry-count")) {
              retryCount = (long) wfRunEntity.getAnnotations().get("boomerang.io/retry-count");
            }
            if (retryCount < wfRunEntity.getRetries()) {
              boolean start = false;
              if (wfRunEntity.getAnnotations().containsKey("boomerang.io/submit-with-start")) {
                start = true;
              }
              retryCount++;
              workflowRunService.retry(wfRunId, start, retryCount);
            }
          }
          lockManager.releaseLock(wfRunId, tokenId);
          LOGGER.debug("[{}] Released WorkflowRun lock", wfRunId);
        }
      }
      return true;
    };
  }

  private void updateStatusAndSaveWorkflow(WorkflowRunEntity workflowExecution, RunStatus status,
      RunPhase phase, Optional<String> message, Object... messageArgs) {
    if (message.isPresent()) {
      workflowExecution
          .setStatusMessage(MessageFormatter.arrayFormat(message.get(), messageArgs).getMessage());
    }
    workflowExecution.setStatus(status);
    workflowExecution.setPhase(phase);
    workflowRunRepository.save(workflowExecution);
  }
}
