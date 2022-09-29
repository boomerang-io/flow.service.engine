package io.boomerang.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
public class ExecutionServiceImpl implements ExecutionService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private DAGUtility dagUtility;
  
  @Autowired
  private TaskService taskService;
  
  @Autowired
  private TaskClient taskClient;

//  @Autowired
//  private WorkflowService workflowService;

  @Override
  public CompletableFuture<Boolean> executeWorkflowVersion(WorkflowEntity workflow, WorkflowRevisionEntity wfRevisionEntity,
      WorkflowRunEntity wfRunEntity) {
    final List<TaskExecution> tasks = dagUtility.createTaskList(workflow.getName(), wfRevisionEntity, wfRunEntity);
    final TaskExecution start = getTaskByType(tasks, TaskType.start);
    final TaskExecution end = getTaskByType(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
    if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
      LOGGER.info("workflow is valid");
      createTaskPlan(tasks, wfRunEntity.getId(), start, end, graph);
      return CompletableFuture
          .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks));
    }
    LOGGER.info("workflow is NOT valid");
    wfRunEntity.setStatus(RunStatus.invalid);
    wfRunEntity.setStatusMessage("Failed to run workflow: Incomplete workflow");
    workflowRunRepository.save(wfRunEntity);
    throw new InvalidWorkflowRuntimeException();
  }

  private void createTaskPlan(List<TaskExecution> tasks, String wfRunId, final TaskExecution start,
      final TaskExecution end, final Graph<String, DefaultEdge> graph) {
    final List<String> nodes =
        GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());
    final List<TaskExecution> tasksToExecute = new LinkedList<>();
    for (final String node : nodes) {
      final TaskExecution taskToAdd =
          tasks.stream().filter(tsk -> node.equals(tsk.getId())).findAny().orElse(null);
      tasksToExecute.add(taskToAdd);
    }

    long order = 1;
    for (final TaskExecution executionTask : tasksToExecute) {
      TaskRunEntity taskRunEntity = new TaskRunEntity();
      taskRunEntity.setWorkflowRunId(wfRunId);
      taskRunEntity.setTaskId(executionTask.getId());
      taskRunEntity.setStatus(RunStatus.notstarted);
      taskRunEntity.setOrder(order);
      taskRunEntity.setTaskName(executionTask.getName());
      taskRunEntity.setTaskType(executionTask.getType());
      if (executionTask.getTemplateRef() != null) {
        taskRunEntity.setTaskTemplateId(executionTask.getTemplateRef());
        taskRunEntity.setTaskTemplateVersion(executionTask.getTemplateVersion());
      }

      taskRunEntity = this.taskRunRepository.save(taskRunEntity);
      executionTask.setRunRef(taskRunEntity.getId());
      order++;
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
        LOGGER.debug("executeWorkflowAsync() - Creating Workflow (" + wfRunEntity.getWorkflowRef() + ")...");

          try {
            List<TaskExecution> nextNodes = this.getTasksDependants(tasksToRun, start);
            for (TaskExecution next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                TaskExecutionRequest taskRequest = new TaskExecutionRequest();
                taskRequest.setTaskRunId(next.getRunRef());
                taskRequest.setWorkflowRunId(wfRunId);
                taskClient.startTask(taskService, next);
                LOGGER.debug("executeWorkflowAsync() - Creating TaskRun (" + next.getRunRef() + ")...");
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

  private List<TaskExecution> getTasksDependants(List<TaskExecution> tasks,
      TaskExecution currentTask) {
    return tasks.stream().filter(t -> t.getDependencies().stream()
        .anyMatch(d -> d.getTaskRef().equals(currentTask.getId())))
        .collect(Collectors.toList());
  }

  private TaskExecution getTaskByType(List<TaskExecution> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getType())).findAny().orElse(null);
  }
}
