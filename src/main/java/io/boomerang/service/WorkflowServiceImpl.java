package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.model.TaskTemplateRevision;
import io.boomerang.data.model.WorkflowRevisionTask;
import io.boomerang.data.model.WorkflowStatus;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.Workflow;
import io.boomerang.model.enums.TaskType;
import io.boomerang.util.TaskMapper;

/*
 * Service implements the CRUD ops on a Workflow
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  @Override
  public Workflow get(String workflowId) {

    Workflow workflow = new Workflow();
    final Optional<WorkflowEntity> OptWfEntity = workflowRepository.findById(workflowId);
    final Optional<WorkflowRevisionEntity> OptWfRevisionEntity =
        workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);

    if (!OptWfEntity.isPresent() || !OptWfRevisionEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
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
  public ResponseEntity<?> create(Workflow workflow) {
    WorkflowEntity wfEntity = new WorkflowEntity();
    // TODO: should I be using a bean copy here?
    wfEntity.setName(workflow.getName());
    wfEntity.setIcon(workflow.getIcon());
    wfEntity.setShortDescription(workflow.getShortDescription());
    wfEntity.setDescription(workflow.getDescription());
    wfEntity.setLabels(workflow.getLabels());
    wfEntity.setAnnotations(workflow.getAnnotations());
    wfEntity.setStatus(WorkflowStatus.active);

    WorkflowRevisionEntity wfRevisionEntity = new WorkflowRevisionEntity();
    wfRevisionEntity.setVersion(1);
    wfRevisionEntity.setChangelog(new ChangeLog("Initial workflow"));
    wfRevisionEntity.setMarkdown(workflow.getMarkdown());
    wfRevisionEntity.setParams(workflow.getParams());
    wfRevisionEntity.setWorkspaces(workflow.getWorkspaces());
    wfRevisionEntity.setTasks(TaskMapper.tasksToListOfRevisionTasks(workflow.getTasks()));
    
    //Check Task Names are unique
    List<String> filteredNames = wfRevisionEntity.getTasks().stream().map(t -> t.getName()).collect(Collectors.toList());
    List<String> uniqueFilteredNames = filteredNames.stream().distinct().collect(Collectors.toList());
    LOGGER.debug("Name sizes: {} -> {}", filteredNames, uniqueFilteredNames);
    
    //Check Task Template references are valid
    for (final WorkflowRevisionTask wfRevisionTask : wfRevisionEntity.getTasks()) {
      if (!TaskType.start.equals(wfRevisionTask.getType())
          && !TaskType.end.equals(wfRevisionTask.getType())) {

        String templateId = wfRevisionTask.getTemplateRef();
        Optional<TaskTemplateEntity> taskTemplate = taskTemplateRepository.findById(templateId);

        //TODO: separate into a shared method with DAGUtility ~line100
        if (taskTemplate.isPresent() && taskTemplate.get().getRevisions() != null) {
          // Set template version to specified or default to currentVersion
          Integer templateVersion =
              wfRevisionTask.getTemplateVersion() != null ? wfRevisionTask.getTemplateVersion()
                  : taskTemplate.get().getCurrentVersion();
          Optional<TaskTemplateRevision> revision = taskTemplate.get().getRevisions().stream()
              .parallel().filter(r -> r.getVersion().equals(templateVersion)).findFirst();
          if (!revision.isPresent()) {
            throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_VERSION, templateId, templateVersion);
          }
        } else {
          throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_REF, templateId);
        }
      }
    }
    wfEntity = workflowRepository.save(wfEntity);
    workflow.setId(wfEntity.getId());
    wfRevisionEntity.setWorkflowRef(wfEntity.getId());
    workflowRevisionRepository.save(wfRevisionEntity);

    return ResponseEntity.ok(workflow);
  }
}
