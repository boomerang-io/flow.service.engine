package io.boomerang.service;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.dag.DAGTask;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.model.DAG;
import io.boomerang.data.model.RunStatus;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.data.model.TaskTemplateRevision;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.exceptions.InvalidWorkflowRuntimeException;
import io.boomerang.model.AbstractKeyValue;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskType;
import io.boomerang.util.GraphProcessor;

@Service
public class ExecutionServiceImpl implements ExecutionService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRunRepository workflowRunRepository;

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  @Autowired
  private DAGUtility dagUtility;

//  @Autowired
//  private WorkflowService workflowService;

  @Override
  public CompletableFuture<Boolean> executeWorkflowVersion(WorkflowRevisionEntity revision,
      WorkflowRunEntity wfRunEntity) {
    final List<TaskExecution> tasks = createTaskList(revision);
    final TaskExecution start = getTaskByType(tasks, TaskType.start);
    final TaskExecution end = getTaskByType(tasks, TaskType.end);
    final Graph<String, DefaultEdge> graph = dagUtility.createGraph(tasks);
    if (dagUtility.validateWorkflow(wfRunEntity, tasks)) {
      createTaskPlan(tasks, wfRunEntity.getId(), start, end, graph);
      return CompletableFuture
          .supplyAsync(executeWorkflowAsync(wfRunEntity.getId(), start, end, graph, tasks));
    }
    wfRunEntity.setStatus(RunStatus.invalid);
    wfRunEntity.setStatusMessage("Failed to run workflow: Incomplete workflow");
    workflowRunRepository.save(wfRunEntity);
    throw new InvalidWorkflowRuntimeException();
  }

  private List<TaskExecution> createTaskList(WorkflowRevisionEntity revisionEntity) {
    final DAG dag = revisionEntity.getDag();

    final List<TaskExecution> taskList = new LinkedList<>();
    for (final DAGTask dagTask : dag.getTasks()) {

      final TaskExecution instanceTask = new TaskExecution();
      // Takes care of duplicating a number of the matching attributes
      BeanUtils.copyProperties(dagTask, instanceTask);
      instanceTask.setId(dagTask.getTaskId());
      instanceTask.setType(dagTask.getTaskType());
      instanceTask.setName(dagTask.getTaskName());
      instanceTask.setWorkflowId(revisionEntity.getWorkflowId());

      if (TaskType.script.equals(dagTask.getTaskType())
          || TaskType.template.equals(dagTask.getTaskType())
          || TaskType.customtask.equals(dagTask.getTaskType())) {

        Integer templateVersion = dagTask.getTemplateVersion();
        String templateId = dagTask.getTemplateId();
        Optional<TaskTemplateEntity> taskTemplate = taskTemplateRepository.findById(templateId);
        if (taskTemplate.isPresent() && taskTemplate.get().getRevisions() != null) {
          List<TaskTemplateRevision> revisions = taskTemplate.get().getRevisions();
          Optional<TaskTemplateRevision> result = revisions.stream().parallel()
              .filter(revision -> revision.getVersion().equals(templateVersion)).findAny();
          if (result.isPresent()) {
            TaskTemplateRevision revision = result.get();
            instanceTask.setRevision(revision);
            instanceTask.setResults(revision.getResults());
          } else {
            Optional<TaskTemplateRevision> latestRevision = revisions.stream()
                .sorted(Comparator.comparingInt(TaskTemplateRevision::getVersion).reversed())
                .findFirst();
            if (latestRevision.isPresent()) {
              instanceTask.setRevision(latestRevision.get());
              instanceTask.setResults(instanceTask.getRevision().getResults());
            }
          }
        } else {
          // TODO: throw more accurate exception
          throw new IllegalArgumentException("Invalid task template selected: " + templateId);
        }

        Map<String, String> inputs = new HashMap<>();
        if (dagTask.getParams() != null) {
          for (AbstractKeyValue param : dagTask.getParams()) {
            inputs.put(param.getKey(), param.getValue());
          }
        }
        instanceTask.setInputs(inputs);

        if (instanceTask.getResults() == null) {
          instanceTask.setResults(dagTask.getResults());
        }
      } else if (TaskType.decision.equals(dagTask.getTaskType())) {
        instanceTask.setDecisionValue(dagTask.getDecisionValue());
      }

      taskList.add(instanceTask);
    }
    return taskList;
  }

  private void createTaskPlan(List<TaskExecution> tasks, String wfRunId, final TaskExecution start,
      final TaskExecution end, final Graph<String, DefaultEdge> graph) {
    final List<String> nodes =
        GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());
    final List<TaskExecution> tasksToRun = new LinkedList<>();
    for (final String node : nodes) {
      final TaskExecution taskToAdd =
          tasks.stream().filter(tsk -> node.equals(tsk.getId())).findAny().orElse(null);
      tasksToRun.add(taskToAdd);
    }

    long order = 1;
    for (final TaskExecution task : tasksToRun) {
      TaskRunEntity taskRunEntity = new TaskRunEntity();
      taskRunEntity.setWorkflowRunId(wfRunId);
      taskRunEntity.setTaskId(task.getId());
      taskRunEntity.setStatus(RunStatus.notstarted);
      taskRunEntity.setOrder(order);
      taskRunEntity.setTaskName(task.getName());
      taskRunEntity.setTaskType(task.getType());
      if (task.getTemplateId() != null) {
        taskRunEntity.setTaskTemplateId(task.getTemplateId());
        taskRunEntity.setTaskTemplateVersion(task.getTemplateVersion());
      }

      taskRunEntity = this.taskRunRepository.save(taskRunEntity);
      task.setRunId(taskRunEntity.getId());
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
        LOGGER.debug("executeWorkflowAsync() - Creating Workflow...");

          try {
            List<TaskExecution> nextNodes = this.getTasksDependants(tasksToRun, start);
            for (TaskExecution next : nextNodes) {
              final List<String> nodes =
                  GraphProcessor.createOrderedTaskList(graph, start.getId(), end.getId());

              if (nodes.contains(next.getId())) {
                TaskExecutionRequest taskRequest = new TaskExecutionRequest();
                taskRequest.setTaskRunId(next.getRunId());
                taskRequest.setWorkflowRunId(wfRunId);
//                TODO: why can't we call taskService.createTask() directly? is it some Async magic
//                taskClient.startTask(taskService, taskRequest);
                LOGGER.debug("executeWorkflowAsync() - Creating Task...");
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
        .anyMatch(d -> d.getTaskId().equals(currentTask.getId())))
        .collect(Collectors.toList());
  }

  private TaskExecution getTaskByType(List<TaskExecution> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getType())).findAny().orElse(null);
  }
}
