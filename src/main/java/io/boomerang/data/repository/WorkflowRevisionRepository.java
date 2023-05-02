package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.WorkflowRevisionEntity;

public interface WorkflowRevisionRepository
    extends MongoRepository<WorkflowRevisionEntity, String> {

  Integer countByWorkflowRef(String workflowRef);

  Optional<WorkflowRevisionEntity> findByWorkflowRefAndVersion(String workflowRef, Integer version);

  Page<WorkflowRevisionEntity> findByWorkflowRef(String string, Pageable pageable);
    
  @Aggregation(pipeline = {
          "{'$match':{'workflowRef': ?0}}",
          "{'$sort': {version: -1}}",
          "{'$limit': 1}"
    })
  Optional<WorkflowRevisionEntity> findByWorkflowRefAndLatestVersion(String workflowRef);
}
