package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.model.enums.TaskTemplateStatus;

public interface TaskTemplateRepository extends MongoRepository<TaskTemplateEntity, String> {

  List<TaskTemplateEntity> findByIdIn(List<String> ids);

  @Override
  List<TaskTemplateEntity> findAll();

  List<TaskTemplateEntity> findByStatus(TaskTemplateStatus active);

  TaskTemplateEntity findByIdAndStatus(String id, TaskTemplateStatus active);

  @Aggregation(pipeline = {"{'$match':{'name': ?0}}", "{'$sort': {version: -1}}", "{'$limit': 1}"})
  Optional<TaskTemplateEntity> findByNameAndLatestVersion(String name);

  Optional<TaskTemplateEntity> findByNameAndVersion(String name, Integer version);
}
