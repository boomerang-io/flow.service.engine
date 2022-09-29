package io.boomerang.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.model.WorkflowStatus;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.exceptions.InvalidWorkflowRuntimeException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.Workflow;
import io.boomerang.util.TaskMapper;

/*
 * Service implements the CRUD ops on a Workflow
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Override
  public Workflow getWorkflow(String workflowId) {

    Workflow workflow = new Workflow();
    final Optional<WorkflowEntity> OptWfEntity = workflowRepository.findById(workflowId);
    final Optional<WorkflowRevisionEntity> OptWfRevisionEntity = workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);

    if (!OptWfEntity.isPresent() || !OptWfRevisionEntity.isPresent()) {
      // TODO: throw a specific exception
      throw new InvalidWorkflowRuntimeException();
    }
    WorkflowEntity wfEntity = OptWfEntity.get();
    WorkflowRevisionEntity wfRevisionEntity = OptWfRevisionEntity.get();

    // TODO: should I be using a bean copy here or mapstruct etc?
    workflow.setId(wfEntity.getId());
    workflow.setName(wfEntity.getName());
    workflow.setIcon(wfEntity.getIcon());
    workflow.setShortDescription(wfEntity.getShortDescription());
    workflow.setDescription(wfEntity.getDescription());
    workflow.setLabels(wfEntity.getLabels());
    workflow.setAnnotations(wfEntity.getAnnotations());
    workflow.setMarkdown(wfRevisionEntity.getMarkdown());
    workflow.setParams(wfRevisionEntity.getParams());
    workflow.setWorkspaces(wfRevisionEntity.getWorkspaces());
    workflow.setTasks(TaskMapper.revisionTasksToListOfTasks(wfRevisionEntity.getTasks()));

    // TODO: filter sensitive inputs/results
    // TODO: handle not found with Exception.
    // TODO: Add in the handling of Workspaces
    // if (workflow.getStorage() == null) {
    // workflow.setStorage(new Storage());
    // }
    // if (workflow.getStorage().getActivity() == null) {
    // workflow.getStorage().setActivity(new ActivityStorage());
    // }
    return workflow;
  }

  /*
   * Adds a new Workflow as WorkflowEntity and WorkflowRevisionEntity
   */
  @Override
  public Workflow addWorkflow(Workflow workflow) {
    WorkflowEntity wfEntity = new WorkflowEntity();
    // TODO: should I be using a bean copy here?
    wfEntity.setName(workflow.getName());
    wfEntity.setIcon(workflow.getIcon());
    wfEntity.setShortDescription(workflow.getShortDescription());
    wfEntity.setDescription(workflow.getDescription());
//    wfEntity.setLabels(ParameterMapper.labelsToKeyValuePairList(workflow.getLabels()));
    wfEntity.setLabels(workflow.getLabels());
//    wfEntity.setAnnotations(ParameterMapper.annotationsToKeyValuePairList(workflow.getAnnotations()));
    wfEntity.setAnnotations(workflow.getAnnotations());
    wfEntity.setStatus(WorkflowStatus.active);
    wfEntity = workflowRepository.save(wfEntity);
    workflow.setId(wfEntity.getId());

    WorkflowRevisionEntity wfRevisionEntity = new WorkflowRevisionEntity();
    wfRevisionEntity.setWorkflowRef(wfEntity.getId());
    wfRevisionEntity.setVersion(1);
    wfRevisionEntity.setChangelog(new ChangeLog("Initial workflow"));
    wfRevisionEntity.setMarkdown(workflow.getMarkdown());
    wfRevisionEntity.setParams(workflow.getParams());
    wfRevisionEntity.setWorkspaces(workflow.getWorkspaces());
    wfRevisionEntity.setTasks(TaskMapper.tasksToListOfRevisionTasks(workflow.getTasks()));
    workflowRevisionRepository.save(wfRevisionEntity);

    return workflow;
  }
}
