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
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
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
  private DAGUtility dagUtility;
  
  @Autowired
  private TaskExecutionService taskService;
  
  @Autowired
  private TaskExecutionClient taskClient;

//  @Autowired
//  private WorkflowService workflowService;

  @Override
  public CompletableFuture<Boolean> executeWorkflowVersion(WorkflowEntity workflow, WorkflowRevisionEntity wfRevisionEntity,
      WorkflowRunEntity wfRunEntity) {
    final List<TaskRunEntity> tasks = dagUtility.createTaskList(wfRevisionEntity, wfRunEntity.getId());
    LOGGER.info("[{}] Found {} tasks: {}", wfRunEntity.getId(), tasks.size(), tasks.toString());
    final TaskRunEntity start = getTaskByType(tasks, TaskType.start);
    final TaskRunEntity end = getTaskByType(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
    if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
      return CompletableFuture
          .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks));
    }
    LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow", wfRunEntity.getId());
    wfRunEntity.setStatus(RunStatus.invalid);
    wfRunEntity.setStatusMessage("Failed to run workflow: incomplete, or invalid, workflow");
    workflowRunRepository.save(wfRunEntity);
    throw new InvalidWorkflowRuntimeException();
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
          wfRunEntity.setStatus(RunStatus.completed);
          wfRunEntity.setCreationDate(new Date());
          workflowRunRepository.save(wfRunEntity);
          return true;
        }

        wfRunEntity.setStatus(RunStatus.inProgress);
        workflowRunRepository.save(wfRunEntity);

//        String workflowId = wfRunEntity.getWorkflowId();
//        WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//        if (workflow != null) {
//          String workflowName = workflow.getName();
//          List<AbstractKeyValue> labels = workflow.getLabels();

          // TODO: Initial Workflow Creation Steps in Controller or Other
          // controllerClient.createFlow(workflowId, workflowName, activityId, enablePVC, labels,
          // executionProperties);
        LOGGER.info("[{}] Executing Workflow Async...", wfRunEntity.getId());

          try {
            List<TaskRunEntity> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
            LOGGER.debug("[{}] Next Nodes Size: {}", wfRunEntity.getId(), nextNodes.size());
            for (TaskRunEntity next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                LOGGER.info("[{}] Creating TaskRun ({})...", wfRunEntity.getId(), next.getId());
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
