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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.model.TaskRun;
import io.boomerang.model.TaskRunEndRequest;
import io.boomerang.model.TaskRunStartRequest;
import io.boomerang.service.TaskRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/taskrun")
@Tag(name = "Task Run",
description = "View, Start, Stop, and Update Status of your Task Runs.")
public class TaskRunV1Controller {

  @Autowired
  private TaskRunService taskRunService;

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Task Runs.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<TaskRunEntity> queryTaskRuns(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "List of statuses to filter for. Defaults to 'ready'.", example = "succeeded,skipped",
      required = false) @RequestParam(defaultValue = "ready", required = false)  Optional<List<String>> status,
      @Parameter(name = "phase",
      description = "List of phases to filter for. Defaults to 'pending'.", example = "completed,finalized",
      required = false) @RequestParam(defaultValue = "pending", required = false)  Optional<List<String>> phase,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    final Pageable pageable = PageRequest.of(page, limit, sort);
    return taskRunService.query(pageable, labels, status, phase);
  }

  @GetMapping(value = "/{taskRunId}")
  @Operation(summary = "Retrieve a specific Task Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskRun> getTaskRuns(
      @Parameter(name = "taskRunId",
      description = "ID of Task Run to Start",
      required = true) @PathVariable(required = true) String taskRunId) {
    return taskRunService.get(taskRunId);
  }

  @PutMapping(value = "/{taskRunId}/start")
  @Operation(summary = "Start a Task Run. The Task Run has to already be queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskRun> startTaskRun(
      @Parameter(name = "taskRunId",
      description = "ID of Task Run to Start",
      required = true) @PathVariable(required = true) String taskRunId,
      @RequestBody Optional<TaskRunStartRequest> taskRunRequest) {
    return taskRunService.start(taskRunId, taskRunRequest);
  }

  @PutMapping(value = "/{taskRunId}/end")
  @Operation(summary = "End the Task Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskRun> endTaskRun(
      @Parameter(name = "taskRunId",
      description = "ID of Task Run to End",
      required = true) @PathVariable(required = true) String taskRunId,
      @RequestBody Optional<TaskRunEndRequest> taskRunRequest) {
    return taskRunService.end(taskRunId, taskRunRequest);
  }

  @PutMapping(value = "/{taskRunId}/cancel")
  @Operation(summary = "Cancel a Task Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskRun> cancelTaskRun(
      @Parameter(name = "taskRunId",
      description = "ID of Task Run to Cancel",
      required = true) @PathVariable(required = true) String taskRunId) {
    return taskRunService.cancel(taskRunId);
  }
}
