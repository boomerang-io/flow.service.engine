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
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.dag.Dependency;
import io.boomerang.data.dag.ExecutionCondition;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.model.RunStatus;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.model.TaskType;
import io.boomerang.repository.TaskRunRepository;
import io.boomerang.util.GraphProcessor;

@Service
public class DAGUtility {

  @Autowired
  private TaskRunRepository taskRunRepository;
  //
  // @Autowired
  // private FlowTaskTemplateService templateService;
  //
  // @Autowired
  // private RevisionService workflowVersionService;
  //

  public boolean validateWorkflow(WorkflowRunEntity wfRunEntity, List<TaskExecution> tasks) {
    final TaskExecution start =
        tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getType())).findAny().orElse(null);
    final TaskExecution end =
        tasks.stream().filter(tsk -> TaskType.end.equals(tsk.getType())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks);
    updateTaskExecutionStatus(graph, tasks, wfRunEntity);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getId());
    return (pathFromStart.getPath(end.getId()) != null);
  }

  public Graph<String, DefaultEdge> createGraph(List<TaskExecution> tasks) {
    final List<String> vertices =
        tasks.stream().map(TaskExecution::getId).collect(Collectors.toList());

    final List<Pair<String, String>> edgeList = new LinkedList<>();
    for (final TaskExecution task : tasks) {
      for (final Dependency dep : task.getDependencies()) {
        final Pair<String, String> pair = Pair.of(dep.getTaskId(), task.getId());
        edgeList.add(pair);
      }
    }
    return GraphProcessor.createGraph(vertices, edgeList);
  }

//  public boolean canCompleteTask(workflowRunEntity wfRunEntity, String taskId) {
//    RevisionEntity revision =
//        workflowVersionService.getWorkflowlWithId(workflowActivity.getWorkflowRevisionid());
//    List<Task> tasks = this.createTaskList(revision, workflowActivity);
//    final Task start = tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getTaskType()))
//        .findAny().orElse(null);
//    final Task current =
//        tasks.stream().filter(tsk -> taskId.equals(tsk.getTaskId())).findAny().orElse(null);
//    Graph<String, DefaultEdge> graph = createGraphAndUpdateStatus(tasks, workflowActivity);
//    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
//    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
//        dijkstraAlg.getPaths(start.getTaskId());
//    return (pathFromStart.getPath(current.getTaskId()) != null);
//  }

  private void updateTaskExecutionStatus(Graph<String, DefaultEdge> graph,
      List<TaskExecution> tasks, WorkflowRunEntity wfRunEntity) {
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);
    while (orderIterator.hasNext()) {
      final String taskId = orderIterator.next();
      TaskExecution currentTask = this.getTaskById(tasks, taskId);
      if (!TaskType.start.equals(currentTask.getType())
          && !TaskType.end.equals(currentTask.getType())) {
        if (currentTask.getRunId() == null) {
          continue;
        }

        Optional<TaskRunEntity> taskRunEntity = taskRunRepository.findById(currentTask.getRunId());

        if (taskRunEntity.isPresent()) {
          RunStatus taskRunStatus = taskRunEntity.get().getStatus();
          if (RunStatus.completed.equals(taskRunStatus)
              || RunStatus.failure.equals(taskRunStatus)) {
            if (TaskType.decision.equals(currentTask.getType())) {
              String switchValue = taskRunEntity.get().getSwitchValue();
              processDecision(graph, tasks, wfRunEntity.getId(), switchValue, currentTask.getId(),
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

  private void processDecision(Graph<String, DefaultEdge> graph, List<TaskExecution> tasksToRun,
      String workflowRunId, String value, final String currentVertex, TaskExecution currentTask) {
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
      List<TaskExecution> tasksToRun, String activityId, String value, final String currentVert,
      TaskExecution currentTask) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);

    List<String> matchedNodes = new LinkedList<>();
    List<String> defaultNodes = new LinkedList<>();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      TaskExecution destTask =
          tasksToRun.stream().filter(t -> t.getId().equals(destination)).findFirst().orElse(null);
      if (destTask != null) {
        Optional<Dependency> optionalDependency = getOptionalDependency(currentVert, destTask);
        if (optionalDependency.isPresent()) {
          Dependency dependency = optionalDependency.get();
          String linkValue = dependency.getSwitchCondition();

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

  private void updateTaskInGraph(Graph<String, DefaultEdge> graph, List<TaskExecution> tasksToRun,
      TaskExecution currentTask) {
    String currentVert = currentTask.getId();
    List<String> matchedNodes = new LinkedList<>();
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);
    RunStatus status = currentTask.getStatus();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      TaskExecution destTask =
          tasksToRun.stream().filter(t -> t.getId().equals(destination)).findFirst().orElse(null);
      if (destTask != null) {
        Optional<Dependency> optionalDependency = getOptionalDependency(currentVert, destTask);
        if (optionalDependency.isPresent()) {
          Dependency dependency = optionalDependency.get();
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

  private Optional<Dependency> getOptionalDependency(final String currentVert,
      TaskExecution destTask) {
    Optional<Dependency> optionalDependency = destTask.getDependencies().stream()
        .filter(d -> d.getTaskId().equals(currentVert)).findAny();
    return optionalDependency;
  }

  private TaskExecution getTaskById(List<TaskExecution> tasks, String id) {
    return tasks.stream().filter(tsk -> id.equals(tsk.getId())).findAny().orElse(null);
  }
}
