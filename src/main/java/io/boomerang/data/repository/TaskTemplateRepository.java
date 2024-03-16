package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.model.enums.TaskTemplateStatus;

@CompoundIndexes({
  @CompoundIndex(name = "name_namespace_idx", def = "{'name' : -1, 'namespace': -1}")
})
public interface TaskTemplateRepository extends MongoRepository<TaskTemplateEntity, String> {
  
  Integer countByName(String name);
  
  Integer countByNameAndStatus(String name, TaskTemplateStatus status);

  Optional<TaskTemplateEntity> findByName(String name);
  
  void deleteByName(String name);
}
