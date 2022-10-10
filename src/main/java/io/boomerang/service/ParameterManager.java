package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.RunParam;

public interface ParameterManager {

  List<RunParam> resolveWorkflowParams(WorkflowRunEntity workflowExecution);

}
