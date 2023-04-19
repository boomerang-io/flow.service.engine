package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.WorkflowRunInsight;
import io.boomerang.model.WorkflowRunRequest;
import io.boomerang.model.WorkflowRunSubmitRequest;
import io.boomerang.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workflowrun")
@Tag(name = "Workflow Run",
description = "Submit, View, Start, End, and Update Status of your Workflow Runs.")
public class WorkflowRunV1Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @GetMapping(value = "/{workflowRunId}")
  @Operation(summary = "Retrieve a specific Workflow Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> getTaskRuns(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue="false") boolean withTasks) {
    return workflowRunService.get(workflowRunId, withTasks);
  }

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Workflow Runs")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowRun> queryWorkflowRuns(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "List of statuses to filter for. Defaults to all.", example = "succeeded,skipped",
      required = false) @RequestParam(required = false)  Optional<List<String>> status,
      @Parameter(name = "phase",
      description = "List of phases to filter for. Defaults to all.", example = "completed,finalized",
      required = false) @RequestParam(required = false)  Optional<List<String>> phase,
      @Parameter(name = "ids",
      description = "List of WorkflowRun IDs  to filter for. Does not validate the IDs provided. Defaults to all.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false)  Optional<List<String>> ids,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    final Pageable pageable = PageRequest.of(page, limit, sort);
    
    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }
    return workflowRunService.query(from, to, pageable, labels, status, phase, ids);
  }
  
  @GetMapping(value = "/insight")
  @Operation(summary = "Retrieve WorkflowRun Insights.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRunInsight> workflowRunInsights(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "ids",
      description = "List of WorkflowRun IDs  to filter for. Does not validate the IDs provided. Defaults to all.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false)  Optional<List<String>> ids,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {

    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }

    return workflowRunService.insights(from, to, labels, ids);
  }

  @PostMapping(value = "/submit")
  @Operation(summary = "Submit a Workflow to be run. Will queue the Workflow Run ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> submitWorkflowRun(
      @Parameter(name = "start",
      description = "Start the Workflow Run immediately after submission",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean start,
      @RequestBody WorkflowRunSubmitRequest request) {
    return workflowRunService.submit(request, start);
  }

  @PutMapping(value = "/{workflowRunId}/start")
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

  @PutMapping(value = "/run/{workflowRunId}/finalize")
  @Operation(summary = "End a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> endWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Finalize",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.finalize(workflowRunId);
  }

  @DeleteMapping(value = "/run/{workflowRunId}/cancel")
  @Operation(summary = "Cancel a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> cancelWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Cancel",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.cancel(workflowRunId);
  }

  @PutMapping(value = "/run/{workflowRunId}/retry")
  @Operation(summary = "Retry Workflow Run execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> retryWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Retry.",
      required = true) @PathVariable(required = true) String workflowRunId,
      @Parameter(name = "start",
      description = "Start the Workflow Run immediately after submission",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean start) {
    return workflowRunService.retry(workflowRunId, start, 1);
  }
}
