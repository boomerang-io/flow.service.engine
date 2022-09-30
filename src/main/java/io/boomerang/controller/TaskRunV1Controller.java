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
@RequestMapping("/api/v1/taskrun")
@Tag(name = "Task Run",
description = "View, Start, Stop, and Update Status of your Task Runs.")
public class TaskRunV1Controller {

  @Autowired
  private WorkflowService workflowService;

  @GetMapping(value = "/query")
  @Operation(summary = "Complete the Task Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<TaskRun> getTaskRuns(@PathVariable String workflowId) {
    return workflowService.getWorkflow(workflowId);
  }

  @PostMapping(value = "/start")
  @Operation(summary = "Start a Task Run. The Task Run has to already be queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow startTaskRun(@RequestBody Workflow workflow) {
    return workflowService.addWorkflow(workflow);
  }

  @PostMapping(value = "/end")
  @Operation(summary = "Complete the Task Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow endTaskRun(@PathVariable String workflowId) {
    return workflowService.getWorkflow(workflowId);
  }
}
