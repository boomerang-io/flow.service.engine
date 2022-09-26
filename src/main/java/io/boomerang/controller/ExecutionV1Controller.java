package io.boomerang.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.service.ExecutionService;

@RestController
@RequestMapping("/apis/v1/workflow/")
public class ExecutionV1Controller {

  @Autowired
  private ExecutionService executionService;

  @PostMapping(value = "/execute/{workflowId}")
  public WorkflowRun executeWorkflow(@PathVariable String workflowId,
      @RequestBody Optional<WorkflowExecutionRequest> executionRequest) {

    return executionService.executeWorkflow(workflowId, executionRequest);
  }
}
