package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.WorkflowTemplateEntity;

public interface WorkflowTemplateRepository extends MongoRepository<WorkflowTemplateEntity, String> {

  @Aggregation(pipeline = {"{'$match':{'name': ?0}}", "{'$sort': {version: -1}}", "{'$limit': 1}"})
  Optional<WorkflowTemplateEntity> findByNameAndLatestVersion(String name);

  Optional<WorkflowTemplateEntity> findByNameAndVersion(String name, Integer version);
  
  void deleteAllByName(String name);
}
