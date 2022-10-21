package io.boomerang.controller;

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
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.Workflow;
import io.boomerang.service.TaskTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/task/template")
@Tag(name = "Task Template Management",
description = "Create and Manage the Task Templates, or Task Definitions.")
public class TaskTemplateV1Controller {

  @Autowired
  private TaskTemplateService taskTemplateService;
  
  @GetMapping(value = "/{taskTemplateId}")
  @Operation(summary = "Retrieve a specific task template. If no version specified, the latest version is returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplate getTaskTemplateWithId(
      @Parameter(name = "taskTemplateId",
      description = "ID of Task Template",
      required = true) @PathVariable String taskTemplateId,
      @Parameter(name = "version",
      description = "Workflow Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return taskTemplateService.get(taskTemplateId, version);
  }

  @PostMapping(value = "/")
  @Operation(summary = "Create a new Task Template")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskTemplate> createWorkflow(@RequestBody TaskTemplate taskTemplate) {
    return taskTemplateService.create(taskTemplate);
  }
}
