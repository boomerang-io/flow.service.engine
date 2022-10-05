package io.boomerang.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.model.TaskTemplateRevision;
import io.boomerang.data.model.WorkflowRevisionTask;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.enums.ExecutionCondition;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.util.GraphProcessor;

@Service
public class DAGUtility {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  public boolean validateWorkflow(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks) {
    final TaskRunEntity start =
        tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getType())).findAny().orElse(null);
    final TaskRunEntity end =
        tasks.stream().filter(tsk -> TaskType.end.equals(tsk.getType())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks);
    updateTaskExecutionStatus(graph, tasks, wfRunEntity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getId());
    return (pathFromStart.getPath(end.getId()) != null);
  }

  public Graph<String, DefaultEdge> createGraph(List<TaskRunEntity> tasks) {
    final List<String> vertices =
        tasks.stream().map(TaskRunEntity::getId).collect(Collectors.toList());

    final List<Pair<String, String>> edgeList = new LinkedList<>();
    for (final TaskRunEntity task : tasks) {
      for (final TaskDependency dep : task.getDependencies()) {
        String depTaskRefAsId = tasks.stream().filter(t -> t.getName().equals(dep.getTaskRef())).findFirst().get().getId();
        final Pair<String, String> pair = Pair.of(depTaskRefAsId, task.getId());
        edgeList.add(pair);
      }
    }
    return GraphProcessor.createGraph(vertices, edgeList);
  }

  public List<TaskRunEntity> createTaskList(WorkflowRevisionEntity wfRevisionEntity,
      String wfRunId) {
    final List<TaskRunEntity> taskList = new LinkedList<>();
    for (final WorkflowRevisionTask wfRevisionTask : wfRevisionEntity.getTasks()) {
      Optional<TaskRunEntity> existingTaskRunEntity =
          taskRunRepository.findFirstByNameAndWorkflowRunRef(wfRevisionTask.getName(), wfRunId);
      if (existingTaskRunEntity.isPresent() && existingTaskRunEntity.get() != null) {
        taskList.add(existingTaskRunEntity.get());
      } else {
        TaskRunEntity executionTask = new TaskRunEntity();
        executionTask.setName(wfRevisionTask.getName());
        executionTask.setStatus(RunStatus.notstarted);
        executionTask.setType(wfRevisionTask.getType());
        executionTask.setTemplateVersion(wfRevisionTask.getTemplateVersion());
        executionTask.setParams(wfRevisionTask.getParams());
        executionTask.setLabels(wfRevisionTask.getLabels());
        executionTask.setAnnotations(wfRevisionTask.getAnnotations());
        executionTask.setDependencies(wfRevisionTask.getDependencies());
        executionTask.setWorkflowRef(wfRevisionEntity.getWorkflowRef());
        executionTask.setWorkflowRevisionRef(wfRevisionEntity.getId());
        executionTask.setWorkflowRunRef(wfRunId);

        if (TaskType.script.equals(wfRevisionTask.getType())
            || TaskType.template.equals(wfRevisionTask.getType())
            || TaskType.customtask.equals(wfRevisionTask.getType())) {

          String templateId = wfRevisionTask.getTemplateRef();
          executionTask.setTemplateRef(templateId);
          Optional<TaskTemplateEntity> taskTemplate = taskTemplateRepository.findById(templateId);

          if (taskTemplate.isPresent() && taskTemplate.get().getRevisions() != null) {
            // Set template version to specified or default to currentVersion
            Integer templateVersion =
                wfRevisionTask.getTemplateVersion() != null ? wfRevisionTask.getTemplateVersion()
                    : taskTemplate.get().getCurrentVersion();
            Optional<TaskTemplateRevision> revision = taskTemplate.get().getRevisions().stream().parallel()
                .filter(r -> r.getVersion().equals(templateVersion)).findFirst();
            if (revision.isPresent()) {
              executionTask.setTemplateResults(revision.get().getResults());
            } else {
           // TODO: throw more accurate exception
              throw new IllegalArgumentException("Invalid task template version selected: " + templateId + "@" + templateVersion);
            }
          } else {
            // TODO: throw more accurate exception
            throw new IllegalArgumentException("Invalid task template selected: " + templateId);
          }
        } else if (TaskType.decision.equals(wfRevisionTask.getType())) {
          executionTask.setDecisionValue(wfRevisionTask.getParams().get("value").toString());
        }

        executionTask = this.taskRunRepository.save(executionTask);
        LOGGER.info("[{}] TaskRunEntity ({}) created for: {}", wfRunId, executionTask.getId(), executionTask.getName());
        taskList.add(executionTask);
      }
    }
    LOGGER.info("[{}] Task List: {}", wfRunId, taskList.toString());
    return taskList;
  }

  public boolean canCompleteTask(List<TaskRunEntity> tasks, TaskRunEntity current) {
    final TaskRunEntity start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getType()))
        .findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getId());
    return (pathFromStart.getPath(current.getId()) != null);
  }

  private void updateTaskExecutionStatus(Graph<String, DefaultEdge> graph,
      List<TaskRunEntity> tasks, WorkflowRunEntity wfRunEntity) {
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);
    while (orderIterator.hasNext()) {
      final String taskId = orderIterator.next();
      TaskRunEntity currentTask = this.getTaskById(tasks, taskId);
      if (!TaskType.start.equals(currentTask.getType())
          && !TaskType.end.equals(currentTask.getType())) {

        Optional<TaskRunEntity> taskRunEntity = taskRunRepository.findById(currentTask.getId());

        if (taskRunEntity.isPresent()) {
          RunStatus taskRunStatus = taskRunEntity.get().getStatus();
          if (RunStatus.completed.equals(taskRunStatus)
              || RunStatus.failure.equals(taskRunStatus)) {
            if (TaskType.decision.equals(currentTask.getType())) {
              String decisionValue = taskRunEntity.get().getDecisionValue();
              processDecision(graph, tasks, wfRunEntity.getId(), decisionValue, currentTask.getId(),
                  currentTask);
            } else {
              currentTask.setStatus(taskRunStatus);
              this.updateTaskInGraph(graph, tasks, currentTask);
            }
          }
        }
      }
    }
  }

  private void processDecision(Graph<String, DefaultEdge> graph, List<TaskRunEntity> tasksToRun,
      String workflowRunId, String value, final String currentVertex, TaskRunEntity currentTask) {
    List<String> removeList =
        calculateNodesToRemove(graph, tasksToRun, workflowRunId, value, currentVertex, currentTask);
    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVertex).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);

      if (source.equals(currentVertex)
          && removeList.stream().noneMatch(str -> str.trim().equals(destination))) {
        graph.removeEdge(e);
      }
    }
  }

  private List<String> calculateNodesToRemove(Graph<String, DefaultEdge> graph,
      List<TaskRunEntity> tasksToRun, String activityId, String value, final String currentVert,
      TaskRunEntity currentTask) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);

    List<String> matchedNodes = new LinkedList<>();
    List<String> defaultNodes = new LinkedList<>();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      TaskRunEntity destTask =
          tasksToRun.stream().filter(t -> t.getId().equals(destination)).findFirst().orElse(null);
      if (destTask != null) {
        Optional<TaskDependency> optionalDependency = getOptionalDependency(currentVert, destTask);
        if (optionalDependency.isPresent()) {
          TaskDependency dependency = optionalDependency.get();
          String linkValue = dependency.getDecisionCondition();

          String node = destTask.getId();

          boolean matched = false;

          if (linkValue != null) {
            String[] lines = linkValue.split("\\r?\\n");
            for (String line : lines) {
              String patternString = line;
              Pattern pattern = Pattern.compile(patternString);
              Matcher matcher = pattern.matcher(value);
              if (matcher.matches()) {
                matched = true;
              }
            }
            if (matched) {
              matchedNodes.add(node);
            }
          } else {
            defaultNodes.add(node);
          }
        }
      }
    }
    List<String> removeList = matchedNodes;
    if (matchedNodes.isEmpty()) {
      removeList = defaultNodes;
    }
    return removeList;
  }

  private void updateTaskInGraph(Graph<String, DefaultEdge> graph, List<TaskRunEntity> tasksToRun,
      TaskRunEntity currentTask) {
    String currentVert = currentTask.getId();
    List<String> matchedNodes = new LinkedList<>();
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);
    RunStatus status = currentTask.getStatus();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      TaskRunEntity destTask =
          tasksToRun.stream().filter(t -> t.getId().equals(destination)).findFirst().orElse(null);
      if (destTask != null) {
        Optional<TaskDependency> optionalDependency = getOptionalDependency(currentVert, destTask);
        if (optionalDependency.isPresent()) {
          TaskDependency dependency = optionalDependency.get();
          ExecutionCondition condition = dependency.getExecutionCondition();
          String node = destTask.getId();
          if (condition != null
              && (RunStatus.failure.equals(status) && ExecutionCondition.failure.equals(condition))
              || (RunStatus.completed.equals(status)
                  && ExecutionCondition.success.equals(condition))
              || (ExecutionCondition.always.equals(condition))) {
            matchedNodes.add(node);
          }
        }
      }
    }

    Iterator<DefaultEdge> itrerator = graph.edgesOf(currentVert).iterator();
    while (itrerator.hasNext()) {
      DefaultEdge e = itrerator.next();
      String destination = graph.getEdgeTarget(e);
      String source = graph.getEdgeSource(e);
      if (source.equals(currentVert)
          && matchedNodes.stream().noneMatch(str -> str.trim().equals(destination))) {
        graph.removeEdge(e);
      }
    }
  }

  private Optional<TaskDependency> getOptionalDependency(final String currentVert,
      TaskRunEntity destTask) {
    Optional<TaskDependency> optionalDependency = destTask.getDependencies().stream()
        .filter(d -> d.getTaskRef().equals(currentVert)).findAny();
    return optionalDependency;
  }

  private TaskRunEntity getTaskById(List<TaskRunEntity> tasks, String id) {
    return tasks.stream().filter(tsk -> id.equals(tsk.getId())).findAny().orElse(null);
  }

  public List<TaskRunEntity> getTasksDependants(List<TaskRunEntity> tasks,
      TaskRunEntity currentTask) {
    return tasks.stream().filter(t -> t.getDependencies().stream()
        .anyMatch(d -> d.getTaskRef().equals(currentTask.getName())))
        .collect(Collectors.toList());
  }
}
