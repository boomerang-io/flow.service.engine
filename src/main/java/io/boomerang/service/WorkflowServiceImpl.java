package io.boomerang.service;

import static java.util.stream.Collectors.groupingBy;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.TaskTemplateRevisionEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.TaskTemplateRevisionRepository;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.Task;
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.Trigger;
import io.boomerang.model.Workflow;
import io.boomerang.model.WorkflowCount;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.WorkflowSubmitRequest;
import io.boomerang.model.WorkflowTrigger;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.boomerang.model.enums.WorkflowStatus;
import io.boomerang.util.ConvertUtil;
import io.boomerang.util.ParameterUtil;

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
  private TaskTemplateRevisionRepository taskTemplateRevisionRepository;

  @Autowired
  private MongoTemplate mongoTemplate;
  
  @Autowired
  private TaskTemplateService taskTemplateService;
  
  @Autowired
  private WorkflowRunService workflowRunService;

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

    Workflow workflow = ConvertUtil.wfEntityToModel(optWfEntity.get(), optWfRevisionEntity.get());
    if (!withTasks) {
      workflow.setTasks(new LinkedList<>()); 
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
          .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(WorkflowStatus.class, q))) {
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
        Workflow w = ConvertUtil.wfEntityToModel(e, optWfRevisionEntity.get());
        // Determine if there are template upgrades available
        w.setUpgradesAvailable(areTemplateUpgradesAvailable(optWfRevisionEntity.get()));
        workflows.add(w);
      }
    });
    
    Page<Workflow> pages = PageableExecutionUtils.getPage(
        workflows, pageable,
        () -> workflows.size());
    LOGGER.debug(pages.toString());
    return pages;
  }
  
  /*
   * Generates Counts for a given set of filters
   */
  @Override
  public ResponseEntity<WorkflowCount> count(Optional<Date> from, Optional<Date> to,
      Optional<List<String>> labels, Optional<List<String>> queryWorkflows) {
    List<Criteria> criteriaList = new ArrayList<>();

    if (from.isPresent() && !to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get());
      criteriaList.add(criteria);
    } else if (!from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").lt(to.get());
      criteriaList.add(criteria);
    } else if (from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get()).lt(to.get());
      criteriaList.add(criteria);
    }

    // TODO add the ability to OR labels not just AND
    if (labels.isPresent()) {
      labels.get().stream().forEach(l -> {
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

    if (queryWorkflows.isPresent()) {
      Criteria criteria = Criteria.where("id").in(queryWorkflows.get());
      criteriaList.add(criteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    LOGGER.debug("Query: " + query.toString());
    List<WorkflowEntity> wfEntities = mongoTemplate.find(query, WorkflowEntity.class);

    // Collate by status count
    Map<String, Long> result = wfEntities.stream()
        .collect(groupingBy(v -> getStatusValue(v), Collectors.counting())); // NOSONAR
    result.put("all", Long.valueOf(wfEntities.size()));

    Arrays.stream(WorkflowStatus.values()).forEach(v -> result.putIfAbsent(v.toString(), 0L));
    
    WorkflowCount wfCount = new WorkflowCount();
    wfCount.setStatus(result);
    return ResponseEntity.ok(wfCount);
  }
  
  private String getStatusValue(WorkflowEntity v) {
    return v.getStatus() == null ? "no_status" : v.getStatus().toString();
  }

  /*
   * Adds a new Workflow as WorkflowEntity and WorkflowRevisionEntity
   */
  @Override
  public ResponseEntity<Workflow> create(Workflow request, boolean useId) {
    WorkflowEntity wfEntity = new WorkflowEntity();
    if (useId) {
      wfEntity.setId(request.getId());
    }
    wfEntity.setName(request.getName());
    wfEntity.setIcon(request.getIcon());
    wfEntity.setDescription(request.getDescription());
    wfEntity.setLabels(request.getLabels());
    // Add System Generated Annotations
    request.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    request.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    wfEntity.setAnnotations(request.getAnnotations());
    wfEntity.setStatus(WorkflowStatus.active);
    wfEntity.setTriggers(request.getTriggers());

    WorkflowRevisionEntity wfRevisionEntity = createWorkflowRevisionEntity(request, 1);
    wfEntity = workflowRepository.save(wfEntity);
    request.setId(wfEntity.getId());
    wfRevisionEntity.setWorkflowRef(wfEntity.getId());
    workflowRevisionRepository.save(wfRevisionEntity);
    //TODO: figure out a better approach to rollback

    // Determine if there are template upgrades available
    Workflow workflow = ConvertUtil.wfEntityToModel(wfEntity, wfRevisionEntity);
    workflow.setUpgradesAvailable(areTemplateUpgradesAvailable(wfRevisionEntity));
    LOGGER.debug(workflow.toString());
    return ResponseEntity.ok(workflow);
  }

  private WorkflowRevisionEntity createWorkflowRevisionEntity(Workflow request, Integer version) {
    WorkflowRevisionEntity wfRevisionEntity = new WorkflowRevisionEntity();
    wfRevisionEntity.setVersion(version);
    ChangeLog changelog = new ChangeLog(version.equals(1) ? CHANGELOG_INITIAL : CHANGELOG_UPDATE);
    if (request.getChangelog() != null) {
      if (request.getChangelog().getAuthor() != null) {
        changelog.setAuthor(request.getChangelog().getAuthor());
      }
      if (request.getChangelog().getReason() != null) {
        changelog.setReason(request.getChangelog().getReason());
      }
      if (request.getChangelog().getDate() != null) {
        changelog.setDate(request.getChangelog().getDate());
      }
    }
    wfRevisionEntity.setChangelog(changelog);
    wfRevisionEntity.setMarkdown(request.getMarkdown());
    wfRevisionEntity.setParams(request.getParams());
    wfRevisionEntity.setWorkspaces(request.getWorkspaces());
    if (request.getTasks() == null || request.getTasks().isEmpty()) {
      List<Task> tasks = new LinkedList<>();
      Task startTask = new Task();
      startTask.setName("start");
      startTask.setType(TaskType.start);
      tasks.add(startTask);
      Task endTask = new Task();
      endTask.setName("end");
      endTask.setType(TaskType.end);
      tasks.add(endTask);
      wfRevisionEntity.setTasks(tasks);
    } else {      
      wfRevisionEntity.setTasks(request.getTasks());
    }
    wfRevisionEntity.setConfig(request.getConfig());
    wfRevisionEntity.setTimeout(request.getTimeout());
    wfRevisionEntity.setRetries(request.getRetries());

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
    for (Task wfTask : wfRevisionEntity.getTasks()) {
      if (!TaskType.start.equals(wfTask.getType())
          && !TaskType.end.equals(wfTask.getType())) {

        //Shared utility with DAGUtility
        TaskTemplate taskTemplate = taskTemplateService.retrieveAndValidateTaskTemplate(wfTask);
        wfTask.setTemplateVersion(taskTemplate.getVersion());
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
    if (!Objects.isNull(workflow.getTriggers())) {
      if (!Objects.isNull(workflow.getTriggers().getManual())) {
        workflowEntity.getTriggers().setManual(workflow.getTriggers().getManual());
      }
      if (!Objects.isNull(workflow.getTriggers().getSchedule())) {
        workflowEntity.getTriggers().setSchedule(workflow.getTriggers().getSchedule());
      }
      if (!Objects.isNull(workflow.getTriggers().getWebhook())) {
        workflowEntity.getTriggers().setWebhook(workflow.getTriggers().getWebhook());
      }
      if (!Objects.isNull(workflow.getTriggers().getEvent())) {
        workflowEntity.getTriggers().setEvent(workflow.getTriggers().getEvent());
      }
      if (!Objects.isNull(workflow.getTriggers().getGithub())) {
        workflowEntity.getTriggers().setGithub(workflow.getTriggers().getGithub());
      }
    }
    // Add System Generated Annotations
    workflowEntity.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    workflowEntity.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
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
    
    Workflow appliedWorkflow = ConvertUtil.wfEntityToModel(workflowEntity, newWorkflowRevisionEntity);
    // Determine if there are template upgrades available
    workflow.setUpgradesAvailable(areTemplateUpgradesAvailable(newWorkflowRevisionEntity));
    return ResponseEntity.ok(appliedWorkflow);
  }
  
  /*
   * Queues the Workflow to be executed (and optionally starts the execution)
   * 
   * Trigger will be set to 'Engine' if empty
   */
  @Override
  public WorkflowRun submit(String workflowId, WorkflowSubmitRequest request, boolean start) {
    logPayload(request);
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    final Optional<WorkflowEntity> optWorkflow =
        workflowRepository.findById(workflowId);
    if (optWorkflow.isEmpty()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    WorkflowEntity workflow = optWorkflow.get();

    // Ensure Workflow is active to be able to be executed
    if (!WorkflowStatus.active.equals(workflow.getStatus())) {
      throw new BoomerangException(BoomerangError.WORKFLOW_NOT_ACTIVE);
    }

    Optional<WorkflowRevisionEntity> optWorkflowRevisionEntity;
    if (request.getWorkflowVersion() != null) {
      optWorkflowRevisionEntity = workflowRevisionRepository
          .findByWorkflowRefAndVersion(workflowId, request.getWorkflowVersion());
    } else {
      optWorkflowRevisionEntity =
          workflowRevisionRepository.findByWorkflowRefAndLatestVersion(workflowId);
    }

    if (!optWorkflowRevisionEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_REVISION_NOT_FOUND);
    }

    LOGGER.debug("Workflow Revision: " + optWorkflowRevisionEntity.get().toString());
    WorkflowRevisionEntity wfRevision = optWorkflowRevisionEntity.get();
    final WorkflowRunEntity wfRunEntity = new WorkflowRunEntity();
    wfRunEntity.setWorkflowRevisionRef(wfRevision.getId());
    wfRunEntity.setWorkflowRef(wfRevision.getWorkflowRef());
    wfRunEntity.setCreationDate(new Date());
    wfRunEntity.setStatus(RunStatus.notstarted);
    wfRunEntity.putLabels(workflow.getLabels());
    wfRunEntity.setParams(ParameterUtil.paramSpecToRunParam(wfRevision.getParams()));
    wfRunEntity.setWorkspaces(wfRevision.getWorkspaces());
    if (!Objects.isNull(wfRevision.getTimeout()) && wfRevision.getTimeout() != 0) {
      wfRunEntity.setTimeout(wfRevision.getTimeout());
    }
    if (!Objects.isNull(wfRevision.getRetries()) && wfRevision.getRetries() != 0) {
      wfRunEntity.setRetries(wfRevision.getRetries());
    }

    // Add values from Run Request if Present
    if (request.getLabels() != null && !request.getLabels().isEmpty()) {
      wfRunEntity.putLabels(request.getLabels());
    }
    if (request.getAnnotations() != null && !request.getAnnotations().isEmpty()) {
      wfRunEntity.putAnnotations(request.getAnnotations());
    }
    if (request.getParams() != null && !request.getParams().isEmpty()) {
      wfRunEntity
          .setParams(ParameterUtil.addUniqueParams(wfRunEntity.getParams(), request.getParams()));
    }
    if (request.getWorkspaces() != null && !request.getWorkspaces().isEmpty()) {
      wfRunEntity.getWorkspaces().addAll(request.getWorkspaces());
    }
    if (!Objects.isNull(request.getTimeout()) && request.getTimeout() != 0) {
      wfRunEntity.setTimeout(request.getTimeout());
    }
    if (!Objects.isNull(request.getRetries()) && request.getRetries() != 0) {
      wfRunEntity.setRetries(request.getRetries());
    }
    // Set Trigger
    if (Objects.isNull(request.getTrigger()) || request.getTrigger().isBlank()) {
      wfRunEntity.setTrigger("Engine");
    } else {
      wfRunEntity.setTrigger(request.getTrigger());
    }
    // Add System Generated Annotations
    Map<String, Object> annotations = new HashMap<>();
    annotations.put("boomerang.io/generation", "4");
    annotations.put("boomerang.io/kind", "WorkflowRun");
    if (start) {
      // Add annotation to know this was created with ?start=true
      wfRunEntity.getAnnotations().put("boomerang.io/submit-with-start", "true");
    }
    wfRunEntity.getAnnotations().putAll(annotations);
    return workflowRunService.run(wfRunEntity, start);
  }
  
  /*
   * Retrieve all the changelogs and return by version
   */
  public ResponseEntity<List<ChangeLogVersion>> changelog(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    final Optional<WorkflowEntity> optWfEntity = workflowRepository.findById(workflowId);
    if (optWfEntity.isPresent()) {
      List<WorkflowRevisionEntity> wfRevisionEntities = workflowRevisionRepository.findByWorkflowRef(workflowId);
      if (wfRevisionEntities.isEmpty()) {
        throw new BoomerangException(BoomerangError.WORKFLOW_REVISION_NOT_FOUND);
      }
      List<ChangeLogVersion> changelogs = new LinkedList<>();
      wfRevisionEntities.forEach(wfRevision -> {
        ChangeLogVersion cl = new ChangeLogVersion();
        cl.setVersion(wfRevision.getVersion());
        cl.setAuthor(wfRevision.getChangelog().getAuthor());
        cl.setReason(wfRevision.getChangelog().getReason());
        cl.setDate(wfRevision.getChangelog().getDate());
        changelogs.add(cl);
      });
      return ResponseEntity.ok(changelogs);
    }

    throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
  }
  
  /*
   * Marks the Workflow as 'deleted' status. This allows WorkflowRuns to still be visualised.
   */
  @Override
  public void delete(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    WorkflowEntity wfEntity = workflowRepository.findById(workflowId).get();
    if (WorkflowStatus.deleted.equals(wfEntity.getStatus())) {
      //TODO: better status to say invalid status. Once deleted you can't move to not deleted.
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    wfEntity.setStatus(WorkflowStatus.deleted);
    wfEntity.setTriggers(new WorkflowTrigger());
    wfEntity.getTriggers().setManual(new Trigger(false));
    workflowRepository.save(wfEntity);
  }

  private boolean areTemplateUpgradesAvailable(WorkflowRevisionEntity wfRevisionEntity) {
    for (Task t : wfRevisionEntity.getTasks()) {
      Optional<TaskTemplateRevisionEntity> taskTemplate =
          taskTemplateRevisionRepository.findByParentAndLatestVersion(t.getTemplateRef());
      if (taskTemplate.isPresent()) {
        if (t.getTemplateVersion() != null
            && (t.getTemplateVersion() < taskTemplate.get().getVersion())) {
          return true;
        }
      }
    }
    return false;
  }

  private void logPayload(WorkflowRunRequest request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writeValueAsString(request);
      LOGGER.info("Received Request Payload: ");
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(e.getStackTrace());
    }
  }
}
