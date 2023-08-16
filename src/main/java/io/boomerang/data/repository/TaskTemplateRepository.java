package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.model.enums.TaskTemplateStatus;

public interface TaskTemplateRepository extends MongoRepository<TaskTemplateEntity, String> {
  Integer countByName(String name);
  Integer countByNameAndStatus(String name, TaskTemplateStatus status);

  Optional<TaskTemplateEntity> findByName(String name);
}
