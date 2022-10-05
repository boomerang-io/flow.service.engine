package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
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
import io.boomerang.data.model.TaskExecution;
import io.boomerang.data.repository.TaskRunRepository;
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
  private TaskRunRepository taskRunRepository;

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
    final List<TaskExecution> tasks = dagUtility.createTaskListFromRevision(workflow.getName(), wfRevisionEntity, wfRunEntity);
    LOGGER.info("[{}] Found {} tasks: {}", wfRunEntity.getId(), tasks.size(), tasks.toString());
    final TaskExecution start = getTaskByType(tasks, TaskType.start);
    final TaskExecution end = getTaskByType(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
    if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
      createTaskPlan(tasks, wfRunEntity.getId(), start, end, graph);

      LOGGER.debug("!!! Task plan created: {}", tasks.toString());
      return CompletableFuture
          .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks));
    }
    LOGGER.error("[{}] Failed to run workflow: incomplete, or invalid, workflow", wfRunEntity.getId());
    wfRunEntity.setStatus(RunStatus.invalid);
    wfRunEntity.setStatusMessage("Failed to run workflow: incomplete, or invalid, workflow");
    workflowRunRepository.save(wfRunEntity);
    throw new InvalidWorkflowRuntimeException();
  }

  private void createTaskPlan(List<TaskExecution> tasks, String wfRunId, final TaskExecution start,
      final TaskExecution end, final Graph<String, DefaultEdge> graph) {
    final List<String> nodes =
        GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());
    LOGGER.debug("!!! Tasks to plan: " + nodes.size());
    final List<TaskExecution> tasksToExecute = new LinkedList<>();
    for (final String node : nodes) {
      final TaskExecution taskToAdd =
          tasks.stream().filter(tsk -> node.equals(tsk.getId())).findAny().orElse(null);
      tasksToExecute.add(taskToAdd);
    }

//    long order = 1;
    for (final TaskExecution executionTask : tasksToExecute) {
      TaskRunEntity taskRunEntity = new TaskRunEntity();
      taskRunEntity.setWorkflowRunRef(wfRunId);
      taskRunEntity.setTaskExecutionRef(executionTask.getId());
      taskRunEntity.setStatus(RunStatus.notstarted);
//      taskRunEntity.setOrder(order);
      taskRunEntity.setTaskName(executionTask.getName());
      taskRunEntity.setTaskType(executionTask.getType());
      if (executionTask.getTemplateRef() != null) {
        taskRunEntity.setTaskTemplateRef(executionTask.getTemplateRef());
        taskRunEntity.setTaskTemplateVersion(executionTask.getTemplateVersion());
      }

      taskRunEntity = this.taskRunRepository.save(taskRunEntity);
      LOGGER.debug("!!! TaskRunEntity ({}) created for: {}", taskRunEntity.getId(), executionTask.getName());
      tasks.get(tasks.indexOf(executionTask)).setRunRef(taskRunEntity.getId());
//      order++;
    }
  }

  private Supplier<Boolean> executeWorkflowAsync(String wfRunId, final TaskExecution start,
      final TaskExecution end, final Graph<String, DefaultEdge> graph,
      final List<TaskExecution> tasksToRun) {
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
            List<TaskExecution> nextNodes = dagUtility.getTasksDependants(tasksToRun, start);
            LOGGER.debug("[{}] Next Nodes Size: {}", wfRunEntity.getId(), nextNodes.size());
            for (TaskExecution next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                LOGGER.info("[{}] Creating TaskRun ({})...", wfRunEntity.getId(), next.getRunRef());
                TaskExecutionRequest taskRequest = new TaskExecutionRequest();
                taskRequest.setTaskRunId(next.getRunRef());
                taskRequest.setWorkflowRunId(wfRunId);
                taskClient.createTask(taskService, next);
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

  private TaskExecution getTaskByType(List<TaskExecution> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getType())).findAny().orElse(null);
  }
}
