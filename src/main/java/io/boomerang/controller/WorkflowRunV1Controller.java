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
import io.boomerang.service.WorkflowExecutionClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workflow/run")
@Tag(name = "Workflow Execution",
description = "Execute your workflows and retrieve the Execution Run.")
public class WorkflowRunV1Controller {

  @Autowired
  private WorkflowExecutionClient executionService;

  @PostMapping(value = "/{workflowId}")
  @Operation(summary = "Execute a specific workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowRun executeWorkflow(@PathVariable String workflowId,
      @RequestBody Optional<WorkflowExecutionRequest> executionRequest) {

    return executionService.executeWorkflow(workflowId, executionRequest);
  }
}
