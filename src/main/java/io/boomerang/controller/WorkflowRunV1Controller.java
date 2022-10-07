package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.WorkflowExecutionRequest;
import io.boomerang.model.WorkflowRun;
import io.boomerang.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workflow/run")
@Tag(name = "Workflow Run",
description = "Execute, View, Start, Stop, and Update Status of your Workflow Runs.")
public class WorkflowRunV1Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @GetMapping(value = "/{workflowRunId}")
  @Operation(summary = "Retrieve a specific Workflow Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> getTaskRuns(@PathVariable String workflowRunId) {
    return workflowRunService.get(workflowRunId);
  }

  //TODO: add status to the query
  @GetMapping(value = "/query")
  @Operation(summary = "Search for Workflow Runs")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<WorkflowRun> queryWorkflowRuns(@Parameter(
      name = "labels",
      description = "Comma separated list of url encoded labels. For example Organization=IBM,customKey=test would be encoded as Organization%3DIBM%2CcustomKey%3Dtest)",
      required = true) @RequestParam(required = true) Optional<String> labels) {

    return workflowRunService.query(labels);
  }

  @PostMapping(value = "/submit")
  @Operation(summary = "Submit a Workflow to be run. Will queue the Workflow Run ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> submitWorkflow(@RequestBody Optional<WorkflowExecutionRequest> executionRequest) {
    return workflowRunService.submit(executionRequest);
  }

  @PostMapping(value = "/start")
  @Operation(summary = "Start Workflow Run execution. The Workflow Run has to already have been queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> startWorkflow(@RequestBody Optional<WorkflowExecutionRequest> executionRequest) {
    return workflowRunService.start(executionRequest);
  }

  @PostMapping(value = "/end")
  @Operation(summary = "End a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> endWorkflow(@RequestBody Optional<WorkflowExecutionRequest> executionRequest) {
    return workflowRunService.end(executionRequest);
  }
}
