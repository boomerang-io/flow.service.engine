package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

  @Override
  public void queue(WorkflowRunEntity workflowExecution) {
    LOGGER.debug("[{}] Recieved queue Workflow request.", workflowExecution.getId());
    // Resolve Parameter Substitutions
    paramManager.resolveWorkflowRunParams(workflowExecution.getId(), workflowExecution.getParams());

    // TODO: do we move the dagUtility.validateWorkflow() here and validate earlier?

    updateStatusAndSaveWorkflow(workflowExecution, RunStatus.ready, RunPhase.pending,
        Optional.empty());
  }

  @Override
  public CompletableFuture<Boolean> start(WorkflowRunEntity wfRunEntity) {
    final Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity =
        this.workflowRevisionRepository.findById(wfRunEntity.getWorkflowRevisionRef());
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity wfRevisionEntity = optWorkflowRevisionEntity.get();
      final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, wfRunEntity);
      LOGGER.info("[{}] Found {} tasks: {}", wfRunEntity.getId(), tasks.size(), tasks.toString());
      final TaskRunEntity start = dagUtility.getTaskByType(tasks, TaskType.start);
      final TaskRunEntity end = dagUtility.getTaskByType(tasks, TaskType.end);
      final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
      if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
        // Set Workflow to Running (Status and Phase). From this point, the duration needs to be
        // calculated.
        wfRunEntity.setStartTime(new Date());
        updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.running, RunPhase.running,
            Optional.empty());
        if (!Objects.isNull(wfRunEntity.getTimeout()) && wfRunEntity.getTimeout() != -1) {
        // Create Timeout Watcher
          LOGGER.info("WorkflowRun Timeout: " + wfRunEntity.getTimeout());
          CompletableFuture
              .supplyAsync(timeoutWorkflowAsync(wfRunEntity), CompletableFuture.delayedExecutor(wfRunEntity.getTimeout(), TimeUnit.MINUTES));
        }
        return CompletableFuture
            .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks));
      }
      updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.invalid, RunPhase.completed,
          Optional.of("Failed to run workflow: incomplete, or invalid, workflow"));
      throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION",
          "[{0}] Failed to run workflow: incomplete, or invalid, workflow",
          HttpStatus.INTERNAL_SERVER_ERROR, wfRunEntity.getId());
    }
    updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.invalid, RunPhase.completed,
        Optional.of("Failed to run workflow: incomplete, or invalid, workflow revision: {}"),
        wfRunEntity.getWorkflowRevisionRef());
    throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION",
        "[{0}] Failed to run workflow: incomplete, or invalid, workflow revision: {1}",
        HttpStatus.INTERNAL_SERVER_ERROR, wfRunEntity.getId(),
        wfRunEntity.getWorkflowRevisionRef());
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

  private Supplier<Boolean> executeWorkflowAsync(String wfRunId, final TaskRunEntity start,
      final TaskRunEntity end, final Graph<String, DefaultEdge> graph,
      final List<TaskRunEntity> tasksToRun) {
    return () -> {
      final Optional<WorkflowRunEntity> optWorkflowRunEntity =
          this.workflowRunRepository.findById(wfRunId);
      if (optWorkflowRunEntity.isPresent()) {
        WorkflowRunEntity workflowRunEntity = optWorkflowRunEntity.get();
        if (tasksToRun.size() == 2) {
          // Workflow only has Start and End and therefore can succeed.
          updateStatusAndSaveWorkflow(workflowRunEntity, RunStatus.succeeded, RunPhase.running,
              Optional.empty());
          return true;
        }
        LOGGER.info("[{}] Executing Workflow Async...", workflowRunEntity.getId());

        try {
          List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
          LOGGER.debug("[{}] Next Nodes Size: {}", workflowRunEntity.getId(), nextNodes.size());
          for (TaskRunEntity next : nextNodes) {
            final List<String> nodes =
                GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

            if (nodes.contains(next.getId())) {
              LOGGER.debug("[{}] Creating TaskRun ({})...", workflowRunEntity.getId(),
                  next.getId());
              taskClient.queueTask(taskService, next);
            }
          }
        } catch (Exception e) {
          updateStatusAndSaveWorkflow(workflowRunEntity, RunStatus.invalid, RunPhase.completed,
              Optional
                  .of("Failed to run workflow: unable to process Workflow and queue all tasks."));
          throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION",
              "[{0}] Failed to run workflow: unable to process Workflow and queue all tasks",
              HttpStatus.INTERNAL_SERVER_ERROR, workflowRunEntity.getId());
        }
      }
      return true;
    };
  }

  /*
   * An async method to execute Timeout checks with DelayedExecutor
   * 
   * The CompletableFuture.orTimeout() method can't be used as the WorkflowRun async thread will
   * finish and hand over to TaskRun threads and thus never reaches timeout.
   * 
   * TODO: determine if this should be used a specific execution thread pool
   * 
   * Implements same locks as TaskExecutionService
   */
  private Supplier<Boolean> timeoutWorkflowAsync(WorkflowRunEntity wfRunEntity) {
    return () -> {
      String wfRunId = wfRunEntity.getId();
      final Optional<WorkflowRunEntity> optWorkflowRunEntity =
          this.workflowRunRepository.findById(wfRunId);
      if (optWorkflowRunEntity.isPresent()) {
        LOGGER.info("[{}] Timeout Workflow Async...", wfRunId);
        //TODO: figure out error. and should Status be failed or timeout.
        if (RunPhase.running.equals(optWorkflowRunEntity.get().getPhase())) {
          List<String> keys = new LinkedList<>();
          keys.add(wfRunId);
          String tokenId = lockManager.acquireWorkflowLock(keys);
          LOGGER.debug("[{}] Obtained WorkflowRun lock", wfRunId);
          
          long duration = new Date().getTime() - optWorkflowRunEntity.get().getStartTime().getTime();
          wfRunEntity.setDuration(duration);
          updateStatusAndSaveWorkflow(wfRunEntity, RunStatus.timedout, RunPhase.completed,
               Optional.of("The WorkflowRun exceeded the timeout. Timeout was set to {0} minutes"), wfRunEntity.getTimeout());
          //TODO: save error block
          //TODO: cancel running tasks
          //TODO: implement retries
          lockManager.releaseWorkflowLock(keys, tokenId);
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
