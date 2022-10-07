package io.boomerang.service;

import org.springframework.stereotype.Service;
import io.boomerang.data.entity.WorkflowRunEntity;


/*
 * Internal client allowing for processing of individual workflows.
 * 
 * Referenced by both the external WorkflowRun Controller and the internal Workflow Execution.
 * 
 * TODO: determine if any of these can be async
 */
@Service
public class WorkflowExecutionClientImpl implements WorkflowExecutionClient {
  
  @Override
  public void queueRevision(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
    workflowExecutionService.queueRevision(wfRunEntity);
  }

  @Override
  public void startRevision(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
      workflowExecutionService.startRevision(wfRunEntity);
  }

  @Override
  public void endRevision(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
      workflowExecutionService.endRevision(wfRunEntity);
  }
}
