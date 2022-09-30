package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.TaskExecutionRequest;
import io.boomerang.model.TaskRun;
import io.boomerang.service.TaskRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/task/run")
@Tag(name = "Task Run",
description = "View, Start, Stop, and Update Status of your Task Runs.")
public class TaskRunV1Controller {

  @Autowired
  private TaskRunService taskRunService;

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Task Runs.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<TaskRun> getTaskRuns(@Parameter(
      name = "labels",
      description = "Comma separated list of url encoded labels. For example Organization=IBM,customKey=test would be encoded as Organization%3DIBM%2CcustomKey%3Dtest)",
      required = true) @RequestParam(required = true) Optional<String> labels) {
    return taskRunService.query(labels);
  }

  @PostMapping(value = "/start")
  @Operation(summary = "Start a Task Run. The Task Run has to already be queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskRun startTaskRun(@RequestBody Optional<TaskExecutionRequest> taskExecutionRequest) {
    return taskRunService.start(taskExecutionRequest);
  }

  @PostMapping(value = "/end")
  @Operation(summary = "Complete the Task Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskRun endTaskRun(@RequestBody Optional<TaskExecutionRequest> taskExecutionRequest) {
    return taskRunService.end(taskExecutionRequest);
  }
}
