package io.boomerang.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.model.TaskTemplateStatus;

public interface TaskTemplateRepository
    extends MongoRepository<TaskTemplateEntity, String> {
  
  List<TaskTemplateEntity> findByIdIn(List<String> ids);

  @Override
  List<TaskTemplateEntity> findAll();

  List<TaskTemplateEntity> findByStatus(TaskTemplateStatus active);

  TaskTemplateEntity findByIdAndStatus(String id, TaskTemplateStatus active);
  
  @Query(value = "{\"scope\" : \"system\"}")
  List<TaskTemplateEntity> findAllSystemTasks();

 
  @Query(value = "{\n"
      + "       $or: [\n"
      + "        {\"scope\" : \"global\"},\n"
      + "        {\"scope\" : null}\n"
      + "    ]}\n")
  List<TaskTemplateEntity> findAllGlobalTasks();
}


