package io.boomerang.engine.repository;

import java.util.Date;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import io.boomerang.engine.entity.ActionEntity;
import io.boomerang.engine.model.enums.ActionStatus;
import io.boomerang.engine.model.enums.ActionType;

public interface ActionRepository extends MongoRepository<ActionEntity, String> {

  ActionEntity findByTaskRunRef(String taskRunRef);

  long countByWorkflowRunRefAndStatus(String workflowRunRef, ActionStatus status);
  
  long countByCreationDateBetween(Date from, Date to);
  
  long countByTypeAndCreationDateBetween(ActionType type, Date from, Date to);

  long countByType(ActionType type);

  long countByStatus(ActionStatus submitted);

  long countByStatusAndCreationDateBetween(ActionStatus submitted, Date date, Date date2);

  long countByStatusAndTypeAndCreationDateBetween(ActionStatus submitted, ActionType type,
      Date date, Date date2);

  long countByStatusAndType(ActionStatus submitted, ActionType type);
  
  void deleteByWorkflowRef(String workflowRef);
  
  void deleteByWorkflowRunRef(String workflowRunRef);
  
  @Query("{'workflowRunRef': ?0 }")
  @Update("{ '$set' : { 'status' : ?1 } }")
  long updateStatusByWorkflowRunRef(String ref, ActionStatus status);
}

