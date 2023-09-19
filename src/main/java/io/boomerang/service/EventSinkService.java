package io.boomerang.service;

import java.util.concurrent.Future;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRunEntity;

public interface EventSinkService {

  Future<Boolean> publishStatusCloudEvent(TaskRunEntity taskRunEntity);

  Future<Boolean> publishStatusCloudEvent(WorkflowRunEntity workflowRunEntity);

  Future<Boolean> publishStatusCloudEvent(WorkflowEntity workflowEntity);

}
