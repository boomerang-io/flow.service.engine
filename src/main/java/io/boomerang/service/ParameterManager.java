package io.boomerang.service;

import java.util.List;
import io.boomerang.model.RunParam;

public interface ParameterManager {

  void resolveWorkflowRunParams(String id, List<RunParam> params);

  void resolveTaskRunParams(String wfRunId, List<RunParam> wfRunParams,
      List<RunParam> taskRunParams);

}
