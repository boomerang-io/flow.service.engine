package io.boomerang.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowRunRepository extends MongoRepository<WorkflowRunEntity, String> {
  
}
