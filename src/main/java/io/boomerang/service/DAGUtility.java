package io.boomerang.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
import io.boomerang.data.model.WorkflowTask;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.TaskDependency;
import io.boomerang.model.enums.ExecutionCondition;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.util.GraphProcessor;
import io.boomerang.util.ParameterUtil;

@Service
public class DAGUtility {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private TaskRunRepository taskRunRepository;

  @Autowired
  private TaskTemplateService taskTemplateService;

  public boolean validateWorkflow(WorkflowRunEntity wfRunEntity, List<TaskRunEntity> tasks) {
    final TaskRunEntity start =
        tasks.stream().filter(tsk -> TaskType.start.equals(tsk.getType())).findAny().orElse(null);
    final TaskRunEntity end =
        tasks.stream().filter(tsk -> TaskType.end.equals(tsk.getType())).findAny().orElse(null);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks);
    updateTaskExecutionStatus(graph, tasks);
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
        try {
          String depTaskRefAsId = tasks.stream().filter(t -> t.getName().equals(dep.getTaskRef()))
              .findFirst().get().getId();
          final Pair<String, String> pair = Pair.of(depTaskRefAsId, task.getId());
          edgeList.add(pair);
        } catch (NoSuchElementException ex) {
          throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_DEPENDENCY, dep.getTaskRef());
        }
      }
    }
    return GraphProcessor.createGraph(vertices, edgeList);
  }

  // TODO: determine a better way to handle the start and end task without saving them as a
  // TaskRunEntity
  public List<TaskRunEntity> createTaskList(WorkflowRevisionEntity wfRevisionEntity,
      WorkflowRunEntity wfRunEntity) {
    final List<TaskRunEntity> taskList = new LinkedList<>();
    for (final WorkflowTask wfRevisionTask : wfRevisionEntity.getTasks()) {
      Optional<TaskRunEntity> existingTaskRunEntity =
          taskRunRepository.findFirstByNameAndWorkflowRunRef(wfRevisionTask.getName(), wfRunEntity.getId());
      if (existingTaskRunEntity.isPresent() && existingTaskRunEntity.get() != null) {
        taskList.add(existingTaskRunEntity.get());
      } else {
        LOGGER.debug("[{}] Creating TaskRunEntity: {}", wfRunEntity.getId(), wfRevisionTask.getName());
        TaskRunEntity taskRunEntity = new TaskRunEntity();
        taskRunEntity.setName(wfRevisionTask.getName());
        taskRunEntity.setStatus(RunStatus.notstarted);
        taskRunEntity.setPhase(RunPhase.pending);
        if (TaskType.start.equals(wfRevisionTask.getType())) {
          taskRunEntity.setStatus(RunStatus.succeeded);
          taskRunEntity.setPhase(RunPhase.completed);
        }
        taskRunEntity.setType(wfRevisionTask.getType());
        taskRunEntity.setCreationDate(new Date());
        taskRunEntity.setTemplateVersion(wfRevisionTask.getTemplateVersion());
        taskRunEntity.setAnnotations(wfRevisionTask.getAnnotations());
        taskRunEntity.setDependencies(wfRevisionTask.getDependencies());
        taskRunEntity.setWorkflowRef(wfRevisionEntity.getWorkflowRef());
        taskRunEntity.setWorkflowRevisionRef(wfRevisionEntity.getId());
        taskRunEntity.setWorkflowRunRef(wfRunEntity.getId());

        if (!TaskType.start.equals(wfRevisionTask.getType())
            && !TaskType.end.equals(wfRevisionTask.getType())) {

          TaskTemplateEntity taskTemplate =
              taskTemplateService.retrieveAndValidateTaskTemplate(wfRevisionTask);
          taskRunEntity.setTemplateRef(wfRevisionTask.getTemplateRef());
          taskRunEntity.setTemplateVersion(taskTemplate.getVersion());
          LOGGER.debug("[{}] Found Task Template: {} ({})", wfRunEntity.getId(), taskTemplate.getName(), taskTemplate.getId());
          
          // Stack the labels based on label propagation
          // Task Template -> Workflow Task -> Run 
          taskRunEntity.getLabels().putAll(taskTemplate.getLabels());
          taskRunEntity.getLabels().putAll(wfRevisionTask.getLabels());
          taskRunEntity.getLabels().putAll(wfRunEntity.getLabels());
          
          //Add System Generated Annotations
          Map<String, Object> annotations = new HashMap<>();
          annotations.put("boomerang.io/generation", "4");
          annotations.put("boomerang.io/kind", "TaskRun");
          taskRunEntity.getAnnotations().putAll(annotations);

          //TODO: validate this actually works - should template results just be merged into Run
          taskRunEntity.setTemplateResults(taskTemplate.getSpec().getResults());
          
          //Set Task RunParams
          if (taskTemplate.getSpec().getParams() != null && !taskTemplate.getSpec().getParams().isEmpty()) {
            LOGGER.debug("[{}] Task Template Params: {}", wfRunEntity.getId(), taskTemplate.getSpec().getParams().toString());
            LOGGER.debug("[{}] Revision Task Params: {}", wfRunEntity.getId(), wfRevisionTask.getParams().toString());
            taskRunEntity.setParams(ParameterUtil.addUniqueParams(
                ParameterUtil.paramSpecToRunParam(taskTemplate.getSpec().getParams()),
                wfRevisionTask.getParams()));
          } else {
            LOGGER.debug("[{}] Task Template Params: {}", wfRunEntity.getId(), wfRevisionTask.getParams().toString());
            taskRunEntity.setParams(wfRevisionTask.getParams());
          }
          LOGGER.debug("[{}] Task Run Params: {}", wfRunEntity.getId(), taskRunEntity.getParams());
          if (!Objects.isNull(wfRevisionTask.getTimeout())
              && wfRevisionTask.getTimeout() != 0) {
            taskRunEntity.setTimeout(wfRevisionTask.getTimeout());
          }          
          //Set TaskRun Spec from TaskTemplate Spec - Debug and Deletion come from an alternate source
          if (!Objects.isNull(taskTemplate.getSpec().getImage())) {
            taskRunEntity.getSpec().setImage(taskTemplate.getSpec().getImage());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getCommand())) {
            taskRunEntity.getSpec().setCommand(taskTemplate.getSpec().getCommand());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getArguments())) {
            taskRunEntity.getSpec().setArguments(taskTemplate.getSpec().getArguments());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getEnvs())) {
            taskRunEntity.getSpec().setEnvs(taskTemplate.getSpec().getEnvs());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getScript())) {
            taskRunEntity.getSpec().setScript(taskTemplate.getSpec().getScript());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getWorkingDir())) {
            taskRunEntity.getSpec().setWorkingDir(taskTemplate.getSpec().getWorkingDir());
          }
          if (!Objects.isNull(taskTemplate.getSpec().getAdditionalProperties())) {
            taskRunEntity.getSpec().getAdditionalProperties().putAll(taskTemplate.getSpec().getAdditionalProperties());
          }
        }
        taskRunRepository.save(taskRunEntity);
        LOGGER.debug("[{}] TaskRunEntity ({}) created for: {}", wfRunEntity.getId(), taskRunEntity.getId(),
            taskRunEntity.getName());
        taskList.add(taskRunEntity);
      }
    }
    LOGGER.info("[{}] Task List: {}", wfRunEntity.getId(), taskList.toString());
    return taskList;
  }

  public boolean canCompleteTask(List<TaskRunEntity> tasks, TaskRunEntity current) {
    final TaskRunEntity start = this.getTaskByType(tasks, TaskType.start);
    Graph<String, DefaultEdge> graph = this.createGraph(tasks);
    updateTaskExecutionStatus(graph, tasks);
    DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
    final SingleSourcePaths<String, DefaultEdge> pathFromStart =
        dijkstraAlg.getPaths(start.getId());
    return (pathFromStart.getPath(current.getId()) != null);
  }

  public TaskRunEntity getTaskByType(List<TaskRunEntity> tasks, TaskType type) {
    return tasks.stream().filter(tsk -> type.equals(tsk.getType())).findAny().orElse(null);
  }

  private void updateTaskExecutionStatus(Graph<String, DefaultEdge> graph,
      List<TaskRunEntity> tasks) {
    TopologicalOrderIterator<String, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<>(graph);
    while (orderIterator.hasNext()) {
      final String taskId = orderIterator.next();
      TaskRunEntity currentTask = this.getTaskById(tasks, taskId);
      if (!TaskType.start.equals(currentTask.getType())
          && !TaskType.end.equals(currentTask.getType())) {

        Optional<TaskRunEntity> taskRunEntity = taskRunRepository.findById(currentTask.getId());

        if (taskRunEntity.isPresent()) {
          RunPhase taskRunPhase = taskRunEntity.get().getPhase();
          LOGGER.debug("[{}] Phase: {}", taskId, taskRunPhase.toString());
          if (RunPhase.completed.equals(taskRunPhase)) {
            if (TaskType.decision.equals(currentTask.getType())) {
              String decisionValue = taskRunEntity.get().getDecisionValue();
              processDecision(graph, tasks, decisionValue, currentTask.getId(), currentTask);
            } else {
              currentTask.setStatus(taskRunEntity.get().getStatus());
              this.updateTaskInGraph(graph, tasks, currentTask);
            }
          }
        }
      }
    }
  }

  private void processDecision(Graph<String, DefaultEdge> graph, List<TaskRunEntity> tasksToRun,
      String value, final String currentVertex, TaskRunEntity currentTask) {
    List<String> removeList =
        calculateNodesToRemove(graph, tasksToRun, value, currentVertex, currentTask);
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
      List<TaskRunEntity> tasksToRun, String value, final String currentVert,
      TaskRunEntity currentTask) {
    Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(currentVert);

    List<String> matchedNodes = new LinkedList<>();
    List<String> defaultNodes = new LinkedList<>();

    for (DefaultEdge edge : outgoingEdges) {
      String destination = graph.getEdgeTarget(edge);
      TaskRunEntity destTask =
          tasksToRun.stream().filter(t -> t.getId().equals(destination)).findFirst().orElse(null);
      if (destTask != null) {
        Optional<TaskDependency> optionalDependency =
            getOptionalDependency(currentTask.getName(), destTask);
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
          LOGGER.debug("[{}] Matched: {}, Decision Value: {}, Condition: {}", currentVert, matched,
              value, linkValue);
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
        Optional<TaskDependency> optionalDependency =
            getOptionalDependency(currentTask.getName(), destTask);
        if (optionalDependency.isPresent()) {
          TaskDependency dependency = optionalDependency.get();
          ExecutionCondition condition = dependency.getExecutionCondition();
          String node = destTask.getId();
          if (condition != null
              && (RunStatus.failed.equals(status) && ExecutionCondition.failure.equals(condition))
              || (RunStatus.succeeded.equals(status)
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
    return tasks.stream()
        .filter(t -> t.getDependencies().stream()
            .anyMatch(d -> d.getTaskRef().equals(currentTask.getName())))
        .collect(Collectors.toList());
  }
}
