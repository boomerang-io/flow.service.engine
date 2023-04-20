package io.boomerang.client;

import io.boomerang.model.ParamLayers;

public interface WorkflowClient {

  ParamLayers getParamLayers(String workflowId);

}
