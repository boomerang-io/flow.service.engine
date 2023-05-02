package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import io.boomerang.data.model.WorkflowRevisionTask;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.enums.TaskTemplateStatus;

@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {
  private static final Logger LOGGER = LogManager.getLogger();
  
  private static final String CHANGELOG_INITIAL = "Initial Task Template";
  
  private static final String CHANGELOG_UPDATE = "Updated Task Template";

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public TaskTemplate get(String name, Optional<Integer> version) {
    Optional<TaskTemplateEntity> taskTemplateEntity;
    if (version.isEmpty()) {
      taskTemplateEntity = taskTemplateRepository.findByNameAndLatestVersion(name);
      if (taskTemplateEntity.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_REF, name, "latest");
      }
    } else {
      taskTemplateEntity = taskTemplateRepository.findByNameAndVersion(name, version.get());
      if (taskTemplateEntity.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_REF, name, version.get());
      }
    }
    TaskTemplate template = new TaskTemplate(taskTemplateEntity.get());
    return template;
  }

  @Override
  public ResponseEntity<TaskTemplate> create(TaskTemplate taskTemplate) {
    //Name Check
    String regex = "^([0-9a-zA-Z\\-]+)$";
    if (!taskTemplate.getName().matches(regex)) {
      throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_NAME, taskTemplate.getName());
    }
    
    //Unique Name Check
    if (taskTemplateRepository.findByNameAndLatestVersion(taskTemplate.getName().toLowerCase()).isPresent()) {
      throw new BoomerangException(BoomerangError.TASK_TEMPLATE_ALREADY_EXISTS, taskTemplate.getName());
    }
    
    //Set Display Name if not provided
    if (taskTemplate.getDisplayName() == null || taskTemplate.getDisplayName().isBlank()) {
      taskTemplate.setDisplayName(taskTemplate.getName());
    }
    
    //TODO additional checks for mandatory fields
    //I.e. if TaskTemplate is of type template, then it must include xyz
    
    taskTemplate.setVersion(1);
    ChangeLog changelog = new ChangeLog(CHANGELOG_INITIAL);
    if (taskTemplate.getChangelog() != null) {
      if (taskTemplate.getChangelog().getAuthor() != null) {
        changelog.setAuthor(taskTemplate.getChangelog().getAuthor());
      }
      if (taskTemplate.getChangelog().getReason() != null) {
        changelog.setReason(taskTemplate.getChangelog().getReason());
      }
      if (taskTemplate.getChangelog().getDate() != null) {
        changelog.setDate(taskTemplate.getChangelog().getDate());
      }
    }
    taskTemplate.setChangelog(changelog);
    taskTemplate.setCreationDate(new Date());
    taskTemplateRepository.save(taskTemplate);
    return ResponseEntity.ok(taskTemplate);
  }
  
  //TODO: handle more of the apply i.e. if original has element, and new does not, keep the original element.
  @Override
  public ResponseEntity<TaskTemplate> apply(TaskTemplate taskTemplate, boolean replace) {
    //Name Check
    String regex = "^([0-9a-zA-Z\\-]+)$";
    if (!taskTemplate.getName().matches(regex)) {
      throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_NAME, taskTemplate.getName());
    }
    
    //Does it already exist?
    Optional<TaskTemplateEntity> taskTemplateEntity = taskTemplateRepository.findByNameAndLatestVersion(taskTemplate.getName().toLowerCase());
    if (!taskTemplateEntity.isPresent()) {
      return this.create(taskTemplate);
    }
    
    //Override Id & version
    if (replace) {
      taskTemplate.setId(taskTemplateEntity.get().getId());
    } else {
      taskTemplate.setId(null);
      taskTemplate.setVersion(taskTemplateEntity.get().getVersion() + 1);
    }
    ChangeLog changelog = new ChangeLog(taskTemplateEntity.get().getVersion().equals(1) ? CHANGELOG_INITIAL : CHANGELOG_UPDATE);
    if (taskTemplate.getChangelog() != null) {
      if (taskTemplate.getChangelog().getAuthor() != null) {
        changelog.setAuthor(taskTemplate.getChangelog().getAuthor());
      }
      if (taskTemplate.getChangelog().getReason() != null) {
        changelog.setReason(taskTemplate.getChangelog().getReason());
      }
      if (taskTemplate.getChangelog().getDate() != null) {
        changelog.setDate(taskTemplate.getChangelog().getDate());
      }
    }
    taskTemplate.setChangelog(changelog);
    taskTemplate.setCreationDate(new Date());
    TaskTemplateEntity savedEntity = taskTemplateRepository.save(taskTemplate);
    TaskTemplate savedTemplate = new TaskTemplate(savedEntity);
    return ResponseEntity.ok(savedTemplate);
  }

  @Override
  public Page<TaskTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames) {
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
            .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(TaskTemplateStatus.class, q))) {
          Criteria criteria = Criteria.where("status").in(queryStatus.get());
          criteriaList.add(criteria);
        } else {
          throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "status");
        }
      }
      
      if (queryNames.isPresent()) {
        Criteria criteria = Criteria.where("name").in(queryNames.get());
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
      
      List<TaskTemplateEntity> taskTemplateEntities = mongoTemplate.find(query.with(pageable), TaskTemplateEntity.class);
      
      List<TaskTemplate> taskTemplates = new LinkedList<>();
      taskTemplateEntities.forEach(e -> taskTemplates.add(new TaskTemplate(e)));

      Page<TaskTemplate> pages = PageableExecutionUtils.getPage(
          taskTemplates, pageable,
          () -> mongoTemplate.count(query, TaskTemplateEntity.class));

      return pages;
  }
  
   @Override
   public void enable(String name) {
     TaskTemplateEntity taskTemplateEntity = this.get(name, Optional.empty());
     taskTemplateEntity.setStatus(TaskTemplateStatus.active);
     taskTemplateRepository.save(taskTemplateEntity);
   }
  
   @Override
   public void disable(String name) {
     TaskTemplateEntity taskTemplateEntity = this.get(name, Optional.empty());
     taskTemplateEntity.setStatus(TaskTemplateStatus.inactive);
     taskTemplateRepository.save(taskTemplateEntity);
   }

   @Override
   public Optional<TaskTemplateEntity> retrieveAndValidateTaskTemplate(
       final WorkflowRevisionTask wfRevisionTask) {
     String templateRef = wfRevisionTask.getTemplateRef();
     Optional<TaskTemplateEntity> taskTemplate;
     if (wfRevisionTask.getTemplateVersion() != null) {
       taskTemplate = taskTemplateRepository.findByNameAndVersion(templateRef,
           wfRevisionTask.getTemplateVersion());
       if (taskTemplate.isEmpty()) {
         throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_REF, templateRef,
             wfRevisionTask.getTemplateVersion());
       } else if (TaskTemplateStatus.inactive.equals(taskTemplate.get().getStatus())) {
         throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INACTIVE_STATUS, templateRef,
             wfRevisionTask.getTemplateVersion());
       }
     } else {
       taskTemplate = taskTemplateRepository.findByNameAndLatestVersion(templateRef);
       if (taskTemplate.isEmpty()) {
         throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INVALID_REF, templateRef,
             "latest");
       } else if (TaskTemplateStatus.inactive.equals(taskTemplate.get().getStatus())) {
         throw new BoomerangException(BoomerangError.TASK_TEMPLATE_INACTIVE_STATUS, templateRef,
             "latest");
       }
     }
     return taskTemplate;
   }
}
