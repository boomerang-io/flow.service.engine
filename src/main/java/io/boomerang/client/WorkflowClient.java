package io.boomerang.client;

import io.boomerang.model.ParamLayers;
import io.boomerang.model.WorkflowSchedule;

public interface WorkflowClient {

  ParamLayers getParamLayers(String workflowId);

  WorkflowSchedule createSchedule(WorkflowSchedule WorkflowSchedule);

}
