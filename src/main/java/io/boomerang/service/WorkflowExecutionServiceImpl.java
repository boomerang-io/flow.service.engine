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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
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
  
  @Value("${flow.engine.mode}")
  private String engineMode;

  @Override
  public void queueRevision(WorkflowRunEntity workflowExecution) {
    LOGGER.debug("[{}] Recieved queue Workflow request.", workflowExecution.getId());
    //Resolve Parameter Substitutions
    paramManager.resolveWorkflowRunParams(workflowExecution.getId(), workflowExecution.getParams());
    //TODO: do we move the dagUtility.validateWorkflow() here and validate earlier?
    
    updateStatusAndSaveWorkflow(workflowExecution, RunStatus.ready, RunPhase.pending, Optional.empty());

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
      final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, workflowExecution);
      LOGGER.info("[{}] Found {} tasks: {}", workflowExecution.getId(), tasks.size(), tasks.toString());
      final TaskRunEntity start = dagUtility.getTaskByType(tasks, TaskType.start);
      final TaskRunEntity end = dagUtility.getTaskByType(tasks, TaskType.end);
      final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
      if (dagUtility.validateWorkflow(workflowExecution, tasks)) {
        //Set Workflow to Running (Status and Phase). From this point, the duration needs to be calculated.
        workflowExecution.setStartTime(new Date());
        updateStatusAndSaveWorkflow(workflowExecution, RunStatus.running, RunPhase.running, Optional.empty());
        return CompletableFuture
            .supplyAsync(executeWorkflowAsync(workflowExecution.getId(), start, end, graph, tasks));
      }
      updateStatusAndSaveWorkflow(workflowExecution, RunStatus.invalid, RunPhase.running, Optional.of("Failed to run workflow: incomplete, or invalid, workflow"));
      throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION", "[{0}] Failed to run workflow: incomplete, or invalid, workflow", HttpStatus.INTERNAL_SERVER_ERROR, workflowExecution.getId());
    }
    updateStatusAndSaveWorkflow(workflowExecution, RunStatus.invalid, RunPhase.running, Optional.of("Failed to run workflow: incomplete, or invalid, workflow revision: {}"), workflowExecution.getWorkflowRevisionRef());
    throw new BoomerangException(1000, "WORKFLOW_RUNTIME_EXCEPTION", "[{0}] Failed to run workflow: incomplete, or invalid, workflow revision: {1}", HttpStatus.INTERNAL_SERVER_ERROR, workflowExecution.getId(), workflowExecution.getWorkflowRevisionRef());
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
          //Workflow only has Start and End and therefore can succeed.
          updateStatusAndSaveWorkflow(workflowExecution, RunStatus.succeeded, RunPhase.running, Optional.empty());
          return true;
        }
        LOGGER.info("[{}] Executing Workflow Async...", workflowExecution.getId());

          try {
            List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
            LOGGER.debug("[{}] Next Nodes Size: {}", workflowExecution.getId(), nextNodes.size());
            for (TaskRunEntity next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                LOGGER.debug("[{}] Creating TaskRun ({})...", workflowExecution.getId(), next.getId());
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
