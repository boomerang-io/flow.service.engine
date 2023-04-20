package io.boomerang.service;

import java.util.Optional;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface ParameterManager {
  void resolveParamLayers(WorkflowRunEntity workflowRunEntity, Optional<TaskRunEntity> optional);
}
