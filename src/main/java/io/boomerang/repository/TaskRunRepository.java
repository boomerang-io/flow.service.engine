package io.boomerang.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskRunEntity;

public interface TaskRunRepository
    extends MongoRepository<TaskRunEntity, String> {

  List<TaskRunEntity> findByWorkflowRunId(String workflowRunId);

  TaskRunEntity findByWorkflowRunIdAndTaskId(String workflowRunId, String taskId);

  TaskRunEntity findByWorkflowRunIdAndTaskName(String workflowRunId, String taskName);
}
