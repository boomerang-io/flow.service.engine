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
import io.boomerang.data.entity.TaskEntity;
import io.boomerang.data.entity.TaskRevisionEntity;
import io.boomerang.data.repository.TaskRepository;
import io.boomerang.data.repository.TaskRevisionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.WorkflowTask;
import io.boomerang.model.Task;
import io.boomerang.model.enums.TaskStatus;

/*
 * Tasks are stored in a main TaskEntity with fields that have limited change scope
 * and a TaskRevisionEntity that holds the versioned elements
 * 
 * It utilises a @DocumentReference for the parent field that allows us to retrieve the TaskEntity from within the TaskRevisionEntity when reading
 */
@Service
public class TaskServiceImpl implements TaskService {
  private static final Logger LOGGER = LogManager.getLogger();
  
  private static final String CHANGELOG_INITIAL = "Initial Task Template";
  private static final String CHANGELOG_UPDATE = "Updated Task Template";
  private static final String NAME_REGEX = "^([0-9a-zA-Z\\-]+)$";
  private static final String ANNOTATION_GENERATION = "4";
  private static final String ANNOTATION_KIND = "Task";

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private TaskRevisionRepository taskRevisionRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public Task get(String id, Optional<Integer> version) {   
    Optional<TaskRevisionEntity> taskRevisionEntity;    
    if (version.isPresent()) {
      taskRevisionEntity = taskRevisionRepository.findByParentRefAndVersion(id, version.get());
    } else {
      taskRevisionEntity = taskRevisionRepository.findByParentRefAndLatestVersion(id);
    }
    if (taskRevisionEntity.isPresent()) {
      Optional<TaskEntity> taskEntity  = taskRepository.findById(id);
      if (taskEntity.isPresent()) {
        return convertEntityToModel(taskEntity.get(), taskRevisionEntity.get());
      }
    }
    throw new BoomerangException(BoomerangError.TASK_INVALID_REF, id, version.isPresent() ? version.get() : "latest");
  }

  /*
   * Create Task
   * 
   * TODO additional checks for mandatory fields
   */
  @Override
  public Task create(Task request) {
    //Remove ID
    request.setId(null);
    
    //Name Check
    if (!request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, request.getName());
    }
    
    //Unique Name Check
    //TODO is this needed
//    if (taskTemplateRepository.countByName(taskTemplate.getName().toLowerCase()) > 0) {
//      throw new BoomerangException(BoomerangError.TASKTEMPLATE_ALREADY_EXISTS, taskTemplate.getName());
//    }
    
    //Set Display Name if not provided
    if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
      request.setDisplayName(request.getName());
    }

    //Set System Generated Annotations
    request.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    request.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    
    //Set as initial version
    request.setVersion(1);
    ChangeLog changelog = new ChangeLog(CHANGELOG_INITIAL);
    updateChangeLog(request, changelog);
    request.setChangelog(changelog);
    
    //Save
    TaskEntity taskTemplateEntity = new TaskEntity(request);
    TaskRevisionEntity taskTemplateRevisionEntity = new TaskRevisionEntity(request);
    taskTemplateRevisionEntity.setParentRef(taskTemplateEntity.getId());
    taskRepository.save(taskTemplateEntity);
    taskRevisionRepository.save(taskTemplateRevisionEntity);
    
    return convertEntityToModel(taskTemplateEntity, taskTemplateRevisionEntity);
  }
  
  //TODO: handle more of the apply i.e. if original has element, and new does not, keep the original element.
  @Override
  public Task apply(Task request, boolean replace) {
    //Name Check
    if (!request.getName().matches(NAME_REGEX)) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, request.getName());
    }

    //Does it already exist?
    Optional<TaskEntity> taskOpt = Optional.empty();
    if (!request.getId().isEmpty()) {
      taskOpt = taskRepository.findById(request.getId());
    }
    if (request.getId().isEmpty() || !taskOpt.isPresent()) {
      return this.create(request);
    }
    TaskEntity taskEntity = taskOpt.get();
    
    //Check for active status
    if (TaskStatus.inactive.equals(taskEntity.getStatus()) && !TaskStatus.active.equals(request.getStatus())) {
      throw new BoomerangException(BoomerangError.TASK_INACTIVE_STATUS, request.getName(), "latest");
    }
    
    //Get latest revision
    Optional<TaskRevisionEntity> taskRevisionEntity = taskRevisionRepository.findByParentRefAndLatestVersion(request.getId());
    if (taskRevisionEntity.isEmpty()) {
      throw new BoomerangException(BoomerangError.TASK_INVALID_REF, request.getName(), "latest");
    }
    
    //Update TaskTemplateEntity
    //Set System Generated Annotations
    //Name (slug), Type, Creation Date, and Verified cannot be updated
    taskEntity.setName(request.getName());
    taskEntity.setStatus(request.getStatus());
    taskEntity.getAnnotations().putAll(request.getAnnotations());
    taskEntity.getAnnotations().put("boomerang.io/generation", ANNOTATION_GENERATION);
    taskEntity.getAnnotations().put("boomerang.io/kind", ANNOTATION_KIND);
    taskEntity.getLabels().putAll(request.getLabels());

    //Create / Replace TaskRevisionEntity
    TaskRevisionEntity newTaskRevisionEntity = new TaskRevisionEntity(request);
    newTaskRevisionEntity.setParentRef(taskEntity.getId());
    if (replace) {
      newTaskRevisionEntity.setId(taskRevisionEntity.get().getId());
      newTaskRevisionEntity.setVersion(taskRevisionEntity.get().getVersion());
    } else {
      newTaskRevisionEntity.setVersion(taskRevisionEntity.get().getVersion() + 1);
    }
    //Set Display Name if not provided
    if (newTaskRevisionEntity.getDisplayName() == null || newTaskRevisionEntity.getDisplayName().isBlank()) {
      newTaskRevisionEntity.setDisplayName(request.getName());
    }

    
    //Update changelog
    ChangeLog changelog = new ChangeLog(taskRevisionEntity.get().getVersion().equals(1) ? CHANGELOG_INITIAL : CHANGELOG_UPDATE);
    updateChangeLog(request, changelog);
    newTaskRevisionEntity.setChangelog(changelog);
    
    //Save entities
    newTaskRevisionEntity.setParentRef(taskEntity.getId());
    TaskEntity savedEntity = taskRepository.save(taskEntity);
    TaskRevisionEntity savedRevision = taskRevisionRepository.save(newTaskRevisionEntity);
    return convertEntityToModel(savedEntity, savedRevision);
  }

  private void updateChangeLog(Task taskTemplate, ChangeLog changelog) {
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
  public Page<Task> query(Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort, Optional<List<String>> queryLabels,
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
            .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(TaskStatus.class, q))) {
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
        Criteria criteria = Criteria.where("_id").in(queryIds.get());
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
      
      List<TaskEntity> taskTemplateEntities = mongoTemplate.find(query.with(pageable), TaskEntity.class);
      
      List<Task> taskTemplates = new LinkedList<>();
      taskTemplateEntities.forEach(e -> {
        Optional<TaskRevisionEntity> taskTemplateRevisionEntity =
            taskRevisionRepository.findByParentRefAndLatestVersion(e.getId());
        if (taskTemplateRevisionEntity.isPresent()) {
          Task tt = convertEntityToModel(e, taskTemplateRevisionEntity.get());
          taskTemplates.add(tt);
        }
      });

      Page<Task> pages = PageableExecutionUtils.getPage(
          taskTemplates, pageable,
          () -> taskTemplates.size());

      return pages;
  }  
  
  /*
   * Retrieve all the changelogs and return by version
   */
  @Override
  public List<ChangeLogVersion> changelog(String id) {
      List<TaskRevisionEntity> taskRevisionEntities = taskRevisionRepository.findByParentRef(id);
      if (taskRevisionEntities.isEmpty()) {
        throw new BoomerangException(BoomerangError.TASK_INVALID_NAME, id);
      }
      List<ChangeLogVersion> changelogs = new LinkedList<>();
      taskRevisionEntities.forEach(v -> {
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
   public Task retrieveAndValidateTask(
       final WorkflowTask wfTask) {
     //Get TaskEntity - this will check valid ref and Version
     Task task = this.get(wfTask.getTaskRef(), Optional.ofNullable(wfTask.getTaskVersion()));
     
     //Check Task Status
     if (TaskStatus.inactive.equals(task.getStatus())) {
       throw new BoomerangException(BoomerangError.TASK_INACTIVE_STATUS, wfTask.getTaskRef(),
           wfTask.getTaskVersion());
     }
     return task;
   }
   
   @Override
   public void delete(String name) {
     taskRevisionRepository.deleteByParentRef(name);
     taskRepository.deleteById(name);
   }

   private Task convertEntityToModel(TaskEntity entity,
       TaskRevisionEntity revision) {
     Task task = new Task();
     BeanUtils.copyProperties(entity, task);
     BeanUtils.copyProperties(revision, task, "id"); // want to keep the TaskEntity ID
     return task;
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
