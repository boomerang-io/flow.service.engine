package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskEntity;
import io.boomerang.model.enums.TaskStatus;

@CompoundIndexes({
  @CompoundIndex(name = "name_namespace_idx", def = "{'name' : -1, 'namespace': -1}")
})
public interface TaskRepository extends MongoRepository<TaskEntity, String> {
  
  Integer countByName(String name);
  
  Integer countByNameAndStatus(String name, TaskStatus status);

  Optional<TaskEntity> findByName(String name);
  
  void deleteByName(String name);
}
