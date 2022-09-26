package io.boomerang.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.entity.WorkflowEntity;
import io.boomerang.entity.model.WorkflowStatus;

public interface WorkflowRepository extends MongoRepository<WorkflowEntity, String> {

  List<WorkflowEntity> findByStatus(WorkflowStatus status);
}
