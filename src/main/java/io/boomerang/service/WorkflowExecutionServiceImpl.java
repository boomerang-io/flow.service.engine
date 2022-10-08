package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.exceptions.InvalidWorkflowRuntimeException;
import io.boomerang.model.TaskExecutionRequest;
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
  private TaskExecutionService taskService;
  
  @Autowired
  private TaskExecutionClient taskClient;
  
  @Autowired
  private TaskRunRepository taskRunRepository;

//  @Autowired
//  private WorkflowService workflowService;
  
  @Value("${flow.engine.mode}")
  private String engineMode;

  @Override
  public void queueRevision(WorkflowRunEntity workflowExecution) {
    LOGGER.debug("[{}] Recieved queue Workflow request.", workflowExecution.getId());
    updateStatusAndSaveWorkflow(workflowExecution, RunStatus.ready, RunPhase.pending, Optional.empty());

    //TODO: do we move the dagUtility.validateWorkflow() here and validate earlier?

//  String workflowId = workflowExecution.getWorkflowId();
//  WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//  if (workflow != null) {
//    String workflowName = workflow.getName();
    
    //If in sync mode, don't wait for external prompt to startRevision
    if ("sync".equals(engineMode)) {
      // controllerClient.createFlow(workflowId, workflowName, workflowExecution.getId(), enablePVC, ,
      // executionProperties);
      startRevision(workflowExecution);
    }
  }

  @Override
  public CompletableFuture<Boolean> startRevision(WorkflowRunEntity workflowExecution) {
    final Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity =
        this.workflowRevisionRepository.findById(workflowExecution.getWorkflowRevisionRef());
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity wfRevisionEntity = optWorkflowRevisionEntity.get();
      final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, workflowExecution.getId());
      LOGGER.info("[{}] Found {} tasks: {}", workflowExecution.getId(), tasks.size(), tasks.toString());
      final TaskRunEntity start = dagUtility.getTaskByType(tasks, TaskType.start);
      final TaskRunEntity end = dagUtility.getTaskByType(tasks, TaskType.end);
      final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
      if (dagUtility.validateWorkflow(workflowExecution, tasks)) {
        return CompletableFuture
            .supplyAsync(executeWorkflowAsync(workflowExecution.getId(), start, end, graph, tasks));
      }
      LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow", workflowExecution.getId());
      updateStatusAndSaveWorkflow(workflowExecution, RunStatus.invalid, RunPhase.running, Optional.of("Failed to run workflow: incomplete, or invalid, workflow"));
      throw new InvalidWorkflowRuntimeException();
    }
    LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow revision: {}", workflowExecution.getId(), workflowExecution.getWorkflowRevisionRef());
    updateStatusAndSaveWorkflow(workflowExecution, RunStatus.invalid, RunPhase.running, Optional.of("Failed to run workflow: incomplete, or invalid, workflow revision: {}"), workflowExecution.getWorkflowRevisionRef());
    throw new InvalidWorkflowRuntimeException();
  }

  @Override
  public void endRevision(WorkflowRunEntity workflowExecution) {
    workflowExecution.setPhase(RunPhase.finalized);
    workflowRunRepository.save(workflowExecution);
  }

  private Supplier<Boolean> executeWorkflowAsync(String wfRunId, final TaskRunEntity start,
      final TaskRunEntity end, final Graph<String, DefaultEdge> graph,
      final List<TaskRunEntity> tasksToRun) {
    return () -> {
      final Optional<WorkflowRunEntity> optworkflowExecution =
          this.workflowRunRepository.findById(wfRunId);
      if (optworkflowExecution.isPresent()) {
        WorkflowRunEntity workflowExecution = optworkflowExecution.get();
        if (tasksToRun.size() == 2) {
          //TODO: validate this
          updateStatusAndSaveWorkflow(workflowExecution, RunStatus.succeeded, RunPhase.running, Optional.empty());
          return true;
        }

        //Set Workflow to Running (Status and Phase). From this point, the duration needs to be calculated.
        workflowExecution.setStartTime(new Date());
        updateStatusAndSaveWorkflow(workflowExecution, RunStatus.running, RunPhase.running, Optional.empty());
        LOGGER.info("[{}] Executing Workflow Async...", workflowExecution.getId());

          try {
            List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
            LOGGER.debug("[{}] Next Nodes Size: {}", workflowExecution.getId(), nextNodes.size());
            for (TaskRunEntity next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                LOGGER.debug("[{}] Creating TaskRun ({})...", workflowExecution.getId(), next.getId());
                TaskExecutionRequest taskRequest = new TaskExecutionRequest();
                taskRequest.setTaskRunId(next.getId());
                taskRequest.setWorkflowRunId(wfRunId);
                taskClient.queueTask(taskService, next);
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
//      }

      return true;
    };
  }

  
  private void updateStatusAndSaveWorkflow(WorkflowRunEntity workflowExecution, RunStatus status, RunPhase phase, Optional<String> message, Object... messageArgs) {
    if (message.isPresent()) {
      workflowExecution.setStatusMessage(MessageFormatter.arrayFormat(message.get(), messageArgs).getMessage());
    }
    workflowExecution.setStatus(status);
    workflowExecution.setPhase(phase);
    workflowRunRepository.save(workflowExecution);
  }
}
