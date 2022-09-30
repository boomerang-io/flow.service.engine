package io.boomerang.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskRunEntity;

public interface TaskRunRepository
    extends MongoRepository<TaskRunEntity, String> {

  List<TaskRunEntity> findByWorkflowRunRef(String workflowRunRef);

  TaskRunEntity findFirstByTaskNameAndWorkflowRunRef(String taskName, String workflowRunRef);
}
