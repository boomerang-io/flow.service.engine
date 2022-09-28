package io.boomerang.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface WorkflowRunRepository extends MongoRepository<WorkflowRunEntity, String> {
  
}
