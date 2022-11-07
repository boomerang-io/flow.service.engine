package io.boomerang.data.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.ActionEntity;
import io.boomerang.model.enums.ActionStatus;
import io.boomerang.model.enums.ActionType;



public interface ActionRepository extends MongoRepository<ActionEntity, String> {

  List<ActionEntity> findByTeamId(String teamId);

  ActionEntity findByTaskActivityId(String id);

  long countByActivityIdAndStatus(String activityId, ActionStatus status);
  
  long countByCreationDateBetween(Date from, Date to);
  long countByTypeAndCreationDateBetween(ActionType type, Date from, Date to);

  long countByType(ActionType type);

  long countByStatus(ActionStatus submitted);

  long countByStatusAndCreationDateBetween(ActionStatus submitted, Date date, Date date2);

  long countByStatusAndTypeAndCreationDateBetween(ActionStatus submitted, ActionType type,
      Date date, Date date2);

  long countByStatusAndType(ActionStatus submitted, ActionType type);
  
}

