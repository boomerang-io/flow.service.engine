package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.entity.WorkflowEntity;
import io.boomerang.entity.WorkflowRevisionEntity;
import io.boomerang.entity.WorkflowRunEntity;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;
import io.boomerang.repository.WorkflowRevisionRepository;

@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRunService workflowRunService;

  @Autowired
  private FlowExecutionService flowExecutionService;

  @Autowired
  private WorkflowRevisionRepository workflowRevisonRepository;

  @Autowired
  private WorkflowService workflowService;

  @Override
  public WorkflowRun executeWorkflow(String workflowId,
      Optional<WorkflowExecutionRequest> executionRequest) {

    final WorkflowEntity workflow = workflowService.getWorkflow(workflowId);

    // TODO: Check if Workflow is active and triggers enabled
    // Throws Execution exception if not able to
    // workflowService.canExecuteWorkflow(workflowId);

    WorkflowExecutionRequest request = null;
    if (executionRequest.isPresent()) {
      request = executionRequest.get();
      logPayload(request);
    } else {
      request = new WorkflowExecutionRequest();
    }

    final WorkflowRevisionEntity entity =
        this.workflowRevisonRepository.findWorkflowByIdAndLatestVersion(workflowId);
    if (entity != null) {
      final WorkflowRunEntity activity = workflowRunService.createFlowActivity(entity.getId(),
          trigger, request, taskWorkspaces, request.getLabels());
      flowExecutionService.executeWorkflowVersion(entity.getId(), activity.getId());

      final List<TaskExecutionRequest> steps = activityService.getTaskExecutions(activity.getId());
      final FlowActivity response = new FlowActivity(activity);
      response.setSteps(steps);
      response.setWorkflowName(workflow.getName());
      response.setShortDescription(workflow.getShortDescription());
      return response;
    } else {
      LOGGER.error("No revision to execute");
    }
    return null;
  }

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
