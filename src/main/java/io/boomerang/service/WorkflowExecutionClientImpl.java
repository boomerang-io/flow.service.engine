package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.data.repository.WorkflowRevisionRepository;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;

/*
 * Executes a workflow.
 * 
 * Notes:
 * - Only the execution service should know about an ExecutionRequest. Do not pass through to other services.
 * - The Engine does not do any Authorization. This is handled by the wrapping Workflow Service
 */
@Service
public class WorkflowExecutionClientImpl implements WorkflowExecutionClient {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRunService workflowRunService;

  @Autowired
  private WorkflowExecutionService executionService;

  @Autowired
  private WorkflowRevisionRepository workflowRevisonRepository;

  @Autowired
  private WorkflowRepository workflowRepository;

  @Autowired
  private WorkflowService workflowService;

  @Override
  public WorkflowRun executeWorkflow(String workflowId,
      Optional<WorkflowExecutionRequest> executionRequest) {

    final Optional<WorkflowEntity> workflow = workflowRepository.findById(workflowId);

    // TODO: Check if Workflow is active and triggers enabled
    // Throws Execution exception if not able to
    // workflowService.canExecuteWorkflow(workflowId);
    
    // TODO: Check Quotas or do as part of above canExecute

    WorkflowExecutionRequest request = null;
    if (executionRequest.isPresent()) {
      request = executionRequest.get();
      logPayload(request);
    } else {
      request = new WorkflowExecutionRequest();
    }

    //TODO: move revision into createRun and only pass parts of the executionRequest through to createRun
    final Optional<WorkflowRevisionEntity> workflowRevisionEntity =
        this.workflowRevisonRepository.findByWorkflowRefAndLatestVersion(workflowId);
    if (workflowRevisionEntity.isPresent()) {
      final WorkflowRunEntity wfRunEntity = workflowRunService.createRun(workflowRevisionEntity.get(),
          request, request.getLabels());
      
      executionService.executeWorkflowVersion(workflow.get(), workflowRevisionEntity.get(), wfRunEntity);

      final List<TaskExecutionResponse> taskRuns = workflowRunService.getTaskExecutions(wfRunEntity.getId());
      final WorkflowRun response = new WorkflowRun(wfRunEntity);
      response.setTasks(taskRuns);
      response.setWorkflowName(workflow.get().getName());
      return response;
    } else {
      LOGGER.error("No revision to execute");
    }
    return null;
  }

  private void logPayload(WorkflowExecutionRequest request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writeValueAsString(request);
      LOGGER.info("Received Request Payload: ");
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(e.getStackTrace());
    }
  }
}
