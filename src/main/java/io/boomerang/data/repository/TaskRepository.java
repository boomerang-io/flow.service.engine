package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskEntity;
import io.boomerang.model.enums.TaskStatus;

public interface TaskRepository extends MongoRepository<TaskEntity, String> {
  
  boolean existsByName(String name);
  
  Integer countByNameAndStatus(String name, TaskStatus status);

  Optional<TaskEntity> findByName(String name);
  
  void deleteByName(String name);
}
