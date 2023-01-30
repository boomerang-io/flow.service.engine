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
  public void queue(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
    workflowExecutionService.queue(wfRunEntity);
  }

  @Override
  public void start(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
      workflowExecutionService.start(wfRunEntity);
  }

  @Override
  public void end(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
      workflowExecutionService.end(wfRunEntity);
  }

  @Override
  public void cancel(WorkflowExecutionService workflowExecutionService, WorkflowRunEntity wfRunEntity) {
      workflowExecutionService.end(wfRunEntity);
  }
}
