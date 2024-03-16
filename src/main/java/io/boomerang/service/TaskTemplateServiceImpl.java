package io.boomerang.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
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
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.entity.TaskTemplateRevisionEntity;
import io.boomerang.data.repository.TaskTemplateRepository;
import io.boomerang.data.repository.TaskTemplateRevisionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.Task;
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.enums.TaskTemplateStatus;

/*
 * TaskTemplates are stored in a main TaskTemplateEntity with fields that have limited change scope
 * and a TaskTemplateRevisionEntity that holds the versioned elements
 * 
 * It utilises a @DocumentReference for the parent field that allows us to retrieve the TaskTemplateEntity from within the TaskTemplateRevisionEntity when reading
 */
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {
  private static final Logger LOGGER = LogManager.getLogger();
  
  private static final String CHANGELOG_INITIAL = "Initial Task Template";
  
  private static final String CHANGELOG_UPDATE = "Updated Task Template";
  
  private static final String NAME_REGEX = "^([0-9a-zA-Z\\-]+)$";
  
  private static final String ANNOTATION_GENERATION = "4";
  private static final String ANNOTATION_KIND = "TaskTemplate";

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

  @Autowired
  private TaskTemplateRevisionRepository taskTemplateRevisionRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public TaskTemplate get(String id, Optional<Integer> version) {   
    Optional<TaskTemplateRevisionEntity> taskTemplateRevisionEntity;    
    if (version.isPresent()) {
      taskTemplateRevisionEntity = taskTemplateRevisionRepository.findByParentRefAndVersion(id, version.get());
    } else {
      taskTemplateRevisionEntity = taskTemplateRevisionRepository.findByParentRefAndLatestVersion(id);
    }
    if (taskTemplateRevisionEntity.isPresent()) {
      Optional<TaskTemplateEntity> taskTemplateEntity  = taskTemplateRepository.findById(id);
      if (taskTemplateEntity.isPresent()) {
        return convertEntityToModel(taskTemplateEntity.get(), taskTemplateRevisionEntity.get());
      }
    }
    throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF, id, version.isPresent() ? version.get() : "latest");
  }

  /*
   * Create TaskTemplate
   * 
   * TODO additional checks for mandatory fields
   * I.e. if TaskTemplate is of type template, then it must include xyz
   */
  @Override
  public TaskTemplate create(TaskTemplate taskTemplate) {
    //Remove ID
    taskTemplate.setId(null);
    
    //Name Check
    if (!taskTemplate.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, taskTemplate.getName());
    }
    
    //Unique Name Check
//    if (taskTemplateRepository.countByName(taskTemplate.getName().toLowerCase()) > 0) {
//      throw new BoomerangException(BoomerangError.TASKTEMPLATE_ALREADY_EXISTS, taskTemplate.getName());
//    }
    
    //Set Display Name if not provided
    if (taskTemplate.getDisplayName() == null || taskTemplate.getDisplayName().isBlank()) {
      taskTemplate.setDisplayName(taskTemplate.getName());
    }

    //Set System Generated Annotations
    taskTemplate.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    taskTemplate.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    
    //Set as initial version
    taskTemplate.setVersion(1);
    ChangeLog changelog = new ChangeLog(CHANGELOG_INITIAL);
    updateChangeLog(taskTemplate, changelog);
    taskTemplate.setChangelog(changelog);
    
    //Save
    TaskTemplateEntity taskTemplateEntity = new TaskTemplateEntity(taskTemplate);
    TaskTemplateRevisionEntity taskTemplateRevisionEntity = new TaskTemplateRevisionEntity(taskTemplate);
    taskTemplateRevisionEntity.setParentRef(taskTemplateEntity.getId());
    taskTemplateRepository.save(taskTemplateEntity);
    taskTemplateRevisionRepository.save(taskTemplateRevisionEntity);
    
    return convertEntityToModel(taskTemplateEntity, taskTemplateRevisionEntity);
  }
  
  //TODO: handle more of the apply i.e. if original has element, and new does not, keep the original element.
  @Override
  public TaskTemplate apply(TaskTemplate taskTemplate, boolean replace) {
    //Name Check
    if (!taskTemplate.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, taskTemplate.getName());
    }

    //Does it already exist?
    Optional<TaskTemplateEntity> taskTemplateEntityOpt = Optional.empty();
    if (!taskTemplate.getId().isEmpty()) {
      taskTemplateEntityOpt = taskTemplateRepository.findById(taskTemplate.getId());
    }
    if (taskTemplate.getId().isEmpty() || !taskTemplateEntityOpt.isPresent()) {
      return this.create(taskTemplate);
    }
    TaskTemplateEntity taskTemplateEntity = taskTemplateEntityOpt.get();
    
    //Check for active status
    if (TaskTemplateStatus.inactive.equals(taskTemplateEntity.getStatus()) && !TaskTemplateStatus.active.equals(taskTemplate.getStatus())) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INACTIVE_STATUS, taskTemplate.getName(), "latest");
    }
    
    //Get latest revision
    Optional<TaskTemplateRevisionEntity> taskTemplateRevisionEntity = taskTemplateRevisionRepository.findByParentRefAndLatestVersion(taskTemplate.getName());
    if (taskTemplateRevisionEntity.isEmpty()) {
      throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_REF, taskTemplate.getName(), "latest");
    }
    
    //Update TaskTemplateEntity
    //Set System Generated Annotations
    //Name (slug), Type, Creation Date, and Verified cannot be updated
    taskTemplateEntity.setName(taskTemplate.getName());
    taskTemplateEntity.setStatus(taskTemplate.getStatus());
    taskTemplateEntity.getAnnotations().putAll(taskTemplate.getAnnotations());
    taskTemplateEntity.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    taskTemplateEntity.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    taskTemplateEntity.getLabels().putAll(taskTemplate.getLabels());

    //Create / Replace TaskTemplateRevisionEntity
    TaskTemplateRevisionEntity newTaskTemplateRevisionEntity = new TaskTemplateRevisionEntity(taskTemplate);
    newTaskTemplateRevisionEntity.setParentRef(taskTemplateEntity.getId());
    if (replace) {
      newTaskTemplateRevisionEntity.setId(taskTemplateRevisionEntity.get().getId());
      newTaskTemplateRevisionEntity.setVersion(taskTemplateRevisionEntity.get().getVersion());
    } else {
      newTaskTemplateRevisionEntity.setVersion(taskTemplateRevisionEntity.get().getVersion() + 1);
    }
    //Set Display Name if not provided
    if (newTaskTemplateRevisionEntity.getDisplayName() == null || newTaskTemplateRevisionEntity.getDisplayName().isBlank()) {
      newTaskTemplateRevisionEntity.setDisplayName(taskTemplate.getName());
    }

    
    //Update changelog
    ChangeLog changelog = new ChangeLog(taskTemplateRevisionEntity.get().getVersion().equals(1) ? CHANGELOG_INITIAL : CHANGELOG_UPDATE);
    updateChangeLog(taskTemplate, changelog);
    newTaskTemplateRevisionEntity.setChangelog(changelog);
    
    //Save entities
    newTaskTemplateRevisionEntity.setParentRef(taskTemplateEntity.getId());
    TaskTemplateEntity savedEntity = taskTemplateRepository.save(taskTemplateEntity);
    TaskTemplateRevisionEntity savedRevision = taskTemplateRevisionRepository.save(newTaskTemplateRevisionEntity);
    return convertEntityToModel(savedEntity, savedRevision);
  }

  private void updateChangeLog(TaskTemplate taskTemplate, ChangeLog changelog) {
    if (taskTemplate.getChangelog() != null) {
      if (taskTemplate.getChangelog().getAuthor() != null && !taskTemplate.getChangelog().getAuthor().isBlank()) {
        changelog.setAuthor(taskTemplate.getChangelog().getAuthor());
      }
      if (taskTemplate.getChangelog().getReason() != null && !taskTemplate.getChangelog().getReason().isBlank()) {
        changelog.setReason(taskTemplate.getChangelog().getReason());
      }
      if (taskTemplate.getChangelog().getDate() != null) {
        changelog.setDate(taskTemplate.getChangelog().getDate());
      }
    }
  }

  @Override
  public Page<TaskTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames, Optional<List<String>> queryIds) {
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
      
      List<TaskTemplateEntity> taskTemplateEntities = mongoTemplate.find(query.with(pageable), TaskTemplateEntity.class);
      
      List<TaskTemplate> taskTemplates = new LinkedList<>();
      taskTemplateEntities.forEach(e -> {
        Optional<TaskTemplateRevisionEntity> taskTemplateRevisionEntity =
            taskTemplateRevisionRepository.findByParentRefAndLatestVersion(e.getId());
        if (taskTemplateRevisionEntity.isPresent()) {
          TaskTemplate tt = convertEntityToModel(e, taskTemplateRevisionEntity.get());
          taskTemplates.add(tt);
        }
      });

      Page<TaskTemplate> pages = PageableExecutionUtils.getPage(
          taskTemplates, pageable,
          () -> taskTemplates.size());

      return pages;
  }  
  
  /*
   * Retrieve all the changelogs and return by version
   */
  @Override
  public List<ChangeLogVersion> changelog(String id) {
      List<TaskTemplateRevisionEntity> taskTemplateRevisionEntities = taskTemplateRevisionRepository.findByParentRef(id);
      if (taskTemplateRevisionEntities.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASKTEMPLATE_INVALID_NAME, id);
      }
      List<ChangeLogVersion> changelogs = new LinkedList<>();
      taskTemplateRevisionEntities.forEach(v -> {
        ChangeLogVersion cl = new ChangeLogVersion();
        cl.setVersion(v.getVersion());
        cl.setAuthor(v.getChangelog().getAuthor());
        cl.setReason(v.getChangelog().getReason());
        cl.setDate(v.getChangelog().getDate());
        changelogs.add(cl);
      });
      return changelogs;
  }

   @Override
   public TaskTemplate retrieveAndValidateTaskTemplate(
       final Task wfTask) {
     //Get TaskTemplateEntity - this will check valid templateRef and Version
     TaskTemplate taskTemplate = this.get(wfTask.getTemplateRef(), Optional.ofNullable(wfTask.getTemplateVersion()));
     
     //Check TaskTemplate Status
     if (TaskTemplateStatus.inactive.equals(taskTemplate.getStatus())) {
       throw new BoomerangException(BoomerangError.TASKTEMPLATE_INACTIVE_STATUS, wfTask.getTemplateRef(),
           wfTask.getTemplateVersion());
     }
     return taskTemplate;
   }
   
   @Override
   public void delete(String name) {
     taskTemplateRevisionRepository.deleteByParentRef(name);
     taskTemplateRepository.deleteById(name);
   }

   private TaskTemplate convertEntityToModel(TaskTemplateEntity entity,
       TaskTemplateRevisionEntity revision) {
     TaskTemplate taskTemplate = new TaskTemplate();
     BeanUtils.copyProperties(entity, taskTemplate);
     BeanUtils.copyProperties(revision, taskTemplate, "id");
     return taskTemplate;
   }
   
//   private String convertId(String prefix, String id) {
//     MessageDigest digest;
//     try {
//       digest = MessageDigest.getInstance("SHA-256");
//       byte[] hash = digest.digest(id.getBytes(StandardCharsets.UTF_8));
//       StringBuilder hexString = new StringBuilder();
//       for (byte element : hash) {
//         String hex = Integer.toHexString(0xff & element);
//         if (hex.length() == 1) {
//           hexString.append('0');
//         }
//         hexString.append(hex);
//       }
//       return prefix + hexString.toString();
//     } catch (NoSuchAlgorithmException e) {
//       return null;
//     }
//   }
}
