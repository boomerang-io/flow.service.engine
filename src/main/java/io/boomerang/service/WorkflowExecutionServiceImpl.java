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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.exceptions.InvalidWorkflowRuntimeException;
import io.boomerang.model.TaskExecutionRequest;
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

//  @Autowired
//  private WorkflowService workflowService;
  
  @Value("${flow.engine.mode}")
  private String engineMode;

  @Override
  public void queueRevision(WorkflowRunEntity workflowExecution) {
    LOGGER.debug("[{}] Recieved queue Workflow request.", workflowExecution.getId());
    workflowExecution.setStatus(RunStatus.queued);
    workflowRunRepository.save(workflowExecution);

    //TODO: do we move the dagUtility.validateWorkflow() here and validate earlier?

//  String workflowId = wfRunEntity.getWorkflowId();
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
        this.workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowExecution.getWorkflowRevisionRef());
    if (optWorkflowRevisionEntity.isPresent()) {
      WorkflowRevisionEntity wfRevisionEntity = optWorkflowRevisionEntity.get();
      final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, workflowExecution.getId());
      LOGGER.info("[{}] Found {} tasks: {}", workflowExecution.getId(), tasks.size(), tasks.toString());
      final TaskRunEntity start = getTaskByType(tasks, TaskType.start);
      final TaskRunEntity end = getTaskByType(tasks, TaskType.end);
      final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
      if (dagUtility.validateWorkflow(workflowExecution, tasks)) {
        return CompletableFuture
            .supplyAsync(executeWorkflowAsync(workflowExecution.getId(), start, end, graph, tasks));
      }
      LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow", workflowExecution.getId());
      workflowExecution.setStatus(RunStatus.invalid);
      workflowExecution.setStatusMessage("Failed to run workflow: incomplete, or invalid, workflow");
      workflowRunRepository.save(workflowExecution);
      throw new InvalidWorkflowRuntimeException();
    }
    LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow revision: {}", workflowExecution.getId(), workflowExecution.getWorkflowRevisionRef());
    workflowExecution.setStatus(RunStatus.invalid);
    workflowExecution.setStatusMessage("Failed to run workflow: incomplete, or invalid, workflow revision: " + workflowExecution.getWorkflowRevisionRef());
    workflowRunRepository.save(workflowExecution);
    throw new InvalidWorkflowRuntimeException();
  }

  @Override
  public void endRevision(WorkflowRunEntity wfRunEntity) {
    // TODO Auto-generated method stub
    
  }

  private Supplier<Boolean> executeWorkflowAsync(String wfRunId, final TaskRunEntity start,
      final TaskRunEntity end, final Graph<String, DefaultEdge> graph,
      final List<TaskRunEntity> tasksToRun) {
    return () -> {
      final Optional<WorkflowRunEntity> optWfRunEntity =
          this.workflowRunRepository.findById(wfRunId);
      if (optWfRunEntity.isPresent()) {
        WorkflowRunEntity wfRunEntity = optWfRunEntity.get();
        wfRunEntity.setCreationDate(new Date());
        if (tasksToRun.size() == 2) {
          wfRunEntity.setStatus(RunStatus.succeeded);
          wfRunEntity.setCreationDate(new Date());
          workflowRunRepository.save(wfRunEntity);
          return true;
        }

        wfRunEntity.setStatus(RunStatus.running);
        workflowRunRepository.save(wfRunEntity);
        LOGGER.info("[{}] Executing Workflow Async...", wfRunEntity.getId());

          try {
            List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
            LOGGER.debug("[{}] Next Nodes Size: {}", wfRunEntity.getId(), nextNodes.size());
            for (TaskRunEntity next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                LOGGER.debug("[{}] Creating TaskRun ({})...", wfRunEntity.getId(), next.getId());
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

  private TaskRunEntity getTaskByType(List<TaskRunEntity> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getType())).findAny().orElse(null);
  }
}
