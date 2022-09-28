package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Workflow;
import io.boomerang.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Workflow Management",
description = "Create, List, and Manage your workflows.")
public class WorkflowV1Controller {

  @Autowired
  private WorkflowService workflowService;

  @PostMapping(value = "/workflow")
  @Operation(summary = "Create a new workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow addWorkflow(@RequestBody Workflow workflow) {
    return workflowService.addWorkflow(workflow);
  }

  @GetMapping(value = "/workflow/{workflowId}")
  @Operation(summary = "Retrieve latest version of the workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow getWorkflow(@PathVariable String workflowId) {
    return workflowService.getWorkflow(workflowId);
  }
}
