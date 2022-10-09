package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workflow")
@Tag(name = "Workflow Run",
description = "Submit, View, Start, End, and Update Status of your Workflow Runs.")
public class WorkflowRunV1Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @GetMapping(value = "/run/{workflowRunId}")
  @Operation(summary = "Retrieve a specific Workflow Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> getTaskRuns(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue="true") boolean withTasks) {
    return workflowRunService.get(workflowRunId, withTasks);
  }

  //TODO: add status to the query
  @GetMapping(value = "/run/query")
  @Operation(summary = "Search for Workflow Runs")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowRunEntity> queryWorkflowRuns(
      @Parameter(name = "labels",
      description = "Comma separated list of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "Comma separated list of statuses to filter for. Defaults to all.", example = "succeeded,skipped",
      required = false) @RequestParam(required = false)  Optional<List<String>> status,
      @Parameter(name = "phase",
      description = "Comma separated list of phases to filter for. Defaults to all.", example = "completed,finalized",
      required = false) @RequestParam(required = false)  Optional<List<String>> phase,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    final Pageable pageable = PageRequest.of(page, limit, sort);
    return workflowRunService.query(pageable, labels, status, phase);
  }

  @PostMapping(value = "/{workflowId}/run/submit")
  @Operation(summary = "Submit a Workflow to be run. Will queue the Workflow Run ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> submitWorkflowRun(
      @Parameter(name = "workflowId",
      description = "ID of Workflow to Request a Run for",
      required = true) @PathVariable(required = true) String workflowId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.submit(workflowId, runRequest);
  }

  @PutMapping(value = "/run/{workflowRunId}/start")
  @Operation(summary = "Start Workflow Run execution. The Workflow Run has to already have been queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> startWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Start",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.start(workflowRunId, runRequest);
  }

  @PutMapping(value = "/run/{workflowRunId}/end")
  @Operation(summary = "End a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> endWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to End",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.end(workflowRunId);
  }

  //TODO implement
  @PutMapping(value = "/run/{workflowRunId}/cancel")
  @Operation(summary = "Cancel a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> cancelWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Cancel",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  //TODO implement
  @PutMapping(value = "/run/{workflowRunId}/retry")
  @Operation(summary = "Retry Workflow Run execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> retryWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Retry.",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
