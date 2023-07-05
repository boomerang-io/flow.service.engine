package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.model.WorkflowTask;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.Workflow;
import io.boomerang.model.WorkflowTrigger;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.model.enums.WorkflowStatus;
import io.boomerang.util.TaskMapper;

/*
 * Service implements the CRUD ops on a Workflow
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {
  private static final Logger LOGGER = LogManager.getLogger();
  
  private static final String CHANGELOG_INITIAL = "Initial Workflow";
  
  private static final String CHANGELOG_UPDATE = "Updated Workflow";
  
  private static final String ANNOTATION_GENERATION = "4";
  private static final String ANNOTATION_KIND = "Workflow";

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowRevisionRepository workflowRevisionRepository;

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  @Autowired
  private MongoTemplate mongoTemplate;
  
  @Autowired
  private TaskTemplateService taskTemplateService;

  @Override
  public ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    final Optional<WorkflowEntity> optWfEntity = workflowRepository.findById(workflowId);
    Optional<WorkflowRevisionEntity> optWfRevisionEntity;
    if (version.isPresent()) {
      optWfRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndVersion(workflowId, version.get());
      if (!optWfRevisionEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.WORKFLOW_REVISION_NOT_FOUND);
      }
    } else {
      optWfRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);
    }
    if (!optWfEntity.isPresent() || !optWfRevisionEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    Workflow workflow = new Workflow(optWfEntity.get(), optWfRevisionEntity.get());
    if (withTasks) {
      workflow.setTasks(TaskMapper.workflowTasksToListOfTasks(optWfRevisionEntity.get().getTasks())); 
    }
    
    // Determine if there are template upgrades available
    workflow.setUpgradesAvailable(areTemplateUpgradesAvailable(optWfRevisionEntity.get()));
    
    return ResponseEntity.ok(workflow);
  }

  @Override
  public Page<Workflow> query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort = Sort.by(new Order(querySort.orElse(Direction.ASC), "creationDate"));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }
    List<Criteria> criteriaList = new ArrayList<>();

    if (queryLabels.isPresent()) {
      queryLabels.get().stream().forEach(l -> {
        String decodedLabel = "";
        try {
          decodedLabel = URLDecoder.decode(l, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new BoomerangException(e, BoomerangError.QUERY_INVALID_FILTERS, "labels");
        }
        LOGGER.debug(decodedLabel.toString());
        String[] label = decodedLabel.split("[=]+");
        Criteria labelsCriteria =
            Criteria.where("labels." + label[0].replace(".", "#")).is(label[1]);
        criteriaList.add(labelsCriteria);
      });
    }

    if (queryStatus.isPresent()) {
      if (queryStatus.get().stream()
          .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(RunStatus.class, q))) {
        Criteria criteria = Criteria.where("status").in(queryStatus.get());
        criteriaList.add(criteria);
      } else {
        throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "status");
      }
    }
    
    if (queryIds.isPresent()) {
      Criteria criteria = Criteria.where("id").in(queryIds.get());
      criteriaList.add(criteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    if (queryLimit.isPresent()) {
      query.with(pageable);
    } else {
      query.with(sort);
    }
    
    LOGGER.debug("Query: " + query.toString());
    List<WorkflowEntity> wfEntities = mongoTemplate.find(query, WorkflowEntity.class);
    
    List<Workflow> workflows = new LinkedList<>();
    wfEntities.forEach(e -> {
      LOGGER.debug("Workflow: " + e.toString());
      Optional<WorkflowRevisionEntity> optWfRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndLatestVersion(e.getId());
      if (optWfRevisionEntity.isPresent()) {
        LOGGER.debug("Revision: " + optWfRevisionEntity.get().toString());
        Workflow w = new Workflow(e, optWfRevisionEntity.get());
        // Determine if there are template upgrades available
        w.setUpgradesAvailable(areTemplateUpgradesAvailable(optWfRevisionEntity.get()));
        workflows.add(w);
      }
    });
    
    Page<Workflow> pages = PageableExecutionUtils.getPage(
        workflows, pageable,
        () -> mongoTemplate.count(query, WorkflowEntity.class));
    LOGGER.debug(pages.toString());
    return pages;
  }

  /*
   * Adds a new Workflow as WorkflowEntity and WorkflowRevisionEntity
   */
  @Override
  public ResponseEntity<Workflow> create(Workflow workflow, boolean useId) {
    WorkflowEntity wfEntity = new WorkflowEntity();
    if (useId) {
      wfEntity.setId(workflow.getId());
    }
    wfEntity.setName(workflow.getName());
    wfEntity.setIcon(workflow.getIcon());
    wfEntity.setDescription(workflow.getDescription());
    wfEntity.setLabels(workflow.getLabels());
    // Add System Generated Annotations
    workflow.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    workflow.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    wfEntity.setAnnotations(workflow.getAnnotations());
    wfEntity.setStatus(WorkflowStatus.active);
    wfEntity.setTriggers(workflow.getTriggers() != null ? workflow.getTriggers() : new WorkflowTrigger());

    WorkflowRevisionEntity wfRevisionEntity = createWorkflowRevisionEntity(workflow, 1);
    wfEntity = workflowRepository.save(wfEntity);
    workflow.setId(wfEntity.getId());
    wfRevisionEntity.setWorkflowRef(wfEntity.getId());
    workflowRevisionRepository.save(wfRevisionEntity);
    //TODO: figure out a better approach to rollback

    // Determine if there are template upgrades available
    workflow.setUpgradesAvailable(areTemplateUpgradesAvailable(wfRevisionEntity));
    return ResponseEntity.ok(workflow);
  }

  private WorkflowRevisionEntity createWorkflowRevisionEntity(Workflow workflow, Integer version) {
    WorkflowRevisionEntity wfRevisionEntity = new WorkflowRevisionEntity();
    wfRevisionEntity.setVersion(version);
    ChangeLog changelog = new ChangeLog(version.equals(1) ? CHANGELOG_INITIAL : CHANGELOG_UPDATE);
    if (workflow.getChangelog() != null) {
      if (workflow.getChangelog().getAuthor() != null) {
        changelog.setAuthor(workflow.getChangelog().getAuthor());
      }
      if (workflow.getChangelog().getReason() != null) {
        changelog.setReason(workflow.getChangelog().getReason());
      }
      if (workflow.getChangelog().getDate() != null) {
        changelog.setDate(workflow.getChangelog().getDate());
      }
    }
    wfRevisionEntity.setChangelog(changelog);
    wfRevisionEntity.setMarkdown(workflow.getMarkdown());
    wfRevisionEntity.setParams(workflow.getParams());
    wfRevisionEntity.setWorkspaces(workflow.getWorkspaces());
    wfRevisionEntity.setTasks(TaskMapper.tasksToListOfWorkflowTasks(workflow.getTasks()));
    wfRevisionEntity.setConfig(workflow.getConfig());
    wfRevisionEntity.setTimeout(workflow.getTimeout());
    wfRevisionEntity.setRetries(workflow.getRetries());

    // Check Task Names are unique
    List<String> filteredNames =
        wfRevisionEntity.getTasks().stream().map(t -> t.getName()).collect(Collectors.toList());
    List<String> uniqueFilteredNames =
        filteredNames.stream().distinct().collect(Collectors.toList());
    LOGGER.debug("Name sizes: {} -> {}", filteredNames, uniqueFilteredNames);
    if (filteredNames.size() != uniqueFilteredNames.size()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_NON_UNIQUE_TASK_NAME);
    }

    // Check Task Template references are valid
    for (final WorkflowTask wfRevisionTask : wfRevisionEntity.getTasks()) {
      if (!TaskType.start.equals(wfRevisionTask.getType())
          && !TaskType.end.equals(wfRevisionTask.getType())) {

        //Shared utility with DAGUtility
        taskTemplateService.retrieveAndValidateTaskTemplate(wfRevisionTask);
      }
    }
    return wfRevisionEntity;
  }

  //TODO: handle more of the apply i.e. if original has element, and new does not, keep the original element.
  @Override
  public ResponseEntity<Workflow> apply(Workflow workflow, Boolean replace) {
    //Apply can create new with specified ID if it exists
    //TODO: add check that ID matches required format for MongoDB
    if (workflow.getId() == null || workflow.getId().isBlank() || workflowRepository.findById(workflow.getId()).isEmpty()) {
        return this.create(workflow, replace);
    }
    
    //Update the Workflow Entity with new details
    WorkflowEntity workflowEntity = workflowRepository.findById(workflow.getId()).get();
    if (WorkflowStatus.deleted.equals(workflowEntity.getStatus())) {
      throw new BoomerangException(BoomerangError.WORKFLOW_DELETED);
    }
    if (workflow.getName()!= null && !workflow.getName().isBlank()) {
      workflowEntity.setName(workflow.getName());
    }
    if (workflow.getStatus()!= null) {
      workflowEntity.setStatus(workflow.getStatus());
    }
    if (workflow.getDescription()!= null && !workflow.getDescription().isBlank()) {
      workflowEntity.setDescription(workflow.getDescription());
    }
    if (workflow.getLabels()!= null && !workflow.getLabels().isEmpty()) {
      if (replace) {
        workflowEntity.setLabels(workflow.getLabels());
      } else {
        workflowEntity.getLabels().putAll(workflow.getLabels());
      }
    }
    if (workflow.getAnnotations()!= null && !workflow.getAnnotations().isEmpty()) {
      if (replace) {
        workflowEntity.setAnnotations(workflow.getAnnotations());
      } else {
        workflowEntity.getAnnotations().putAll(workflow.getAnnotations());
      }
    }
    // Add System Generated Annotations
    workflowEntity.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    workflowEntity.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    if (workflow.getTriggers() != null) {
      workflowEntity.getTriggers();
    }
    workflowRepository.save(workflowEntity);
    
    //TODO, the creation of new better to include fields available on the old that aren't available on the new.
    WorkflowRevisionEntity workflowRevisionEntity = workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflow.getId()).get();
    Integer version = workflowRevisionEntity.getVersion();
    WorkflowRevisionEntity newWorkflowRevisionEntity = null;
    if (!replace) {
      version++;
    }
    newWorkflowRevisionEntity = createWorkflowRevisionEntity(workflow, version);
    if (replace) {
      newWorkflowRevisionEntity.setId(workflowRevisionEntity.getId());
    }
    newWorkflowRevisionEntity.setWorkflowRef(workflowRevisionEntity.getWorkflowRef());
    
    workflowRevisionRepository.save(newWorkflowRevisionEntity);
    
    Workflow appliedWorkflow = new Workflow(workflowEntity, newWorkflowRevisionEntity);
    appliedWorkflow.setTasks(TaskMapper.workflowTasksToListOfTasks(newWorkflowRevisionEntity.getTasks()));

    // Determine if there are template upgrades available
    workflow.setUpgradesAvailable(areTemplateUpgradesAvailable(newWorkflowRevisionEntity));
    return ResponseEntity.ok(appliedWorkflow);
  }
  
  /*
   * Marks the Workflow as 'active' status.
   */
  @Override
  public void enable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    updateWorkflowStatus(workflowId, WorkflowStatus.active);
  }
  
  /*
   * Marks the Workflow as 'inactive' status.
   */
  @Override
  public void disable(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    updateWorkflowStatus(workflowId, WorkflowStatus.inactive);
  }
  
  /*
   * Marks the Workflow as 'deleted' status. This allows WorkflowRuns to still be visualised.
   */
  @Override
  public void delete(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    updateWorkflowStatus(workflowId, WorkflowStatus.deleted);
  }

  private boolean areTemplateUpgradesAvailable(WorkflowRevisionEntity wfRevisionEntity) {
    for (WorkflowTask t : wfRevisionEntity.getTasks()) {
      Optional<TaskTemplateEntity> taskTemplate =
          taskTemplateRepository.findByNameAndLatestVersion(t.getTemplateRef());
      if (taskTemplate.isPresent()) {
        if (t.getTemplateVersion() != null
            && (t.getTemplateVersion() < taskTemplate.get().getVersion())) {
          return true;
        }
      }
    }
    return false;
  }
  
  private void updateWorkflowStatus(String workflowId, WorkflowStatus workflowStatus) {
    try {
      WorkflowEntity wfEntity = workflowRepository.findById(workflowId).get();
      if (WorkflowStatus.deleted.equals(wfEntity.getStatus())) {
        //TODO: better status to say invalid status. Once deleted you can't move to not deleted.
        throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
      }
      wfEntity.setStatus(workflowStatus);
      workflowRepository.save(wfEntity);
    } catch (Exception e) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
  }
}
