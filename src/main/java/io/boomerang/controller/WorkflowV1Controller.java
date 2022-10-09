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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.model.Workflow;
import io.boomerang.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workflow")
@Tag(name = "Workflow Management",
description = "Create, List, and Manage your workflows.")
public class WorkflowV1Controller {

  @Autowired
  private WorkflowService workflowService;

  @GetMapping(value = "/{workflowId}")
  @Operation(summary = "Retrieve a version of the workflow. Defaults to latest.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> getWorkflow(
      @Parameter(name = "workflowId",
      description = "ID of Workflow",
      required = true) @PathVariable String workflowId,
      @Parameter(name = "version",
      description = "Workflow Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return workflowService.get(workflowId, version);
  }

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Workflows")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowEntity> queryWorkflows(
      @Parameter(name = "labels",
      description = "Comma separated list of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "Comma separated list of statuses to filter for. Defaults to all.", example = "active,archived",
      required = false) @RequestParam(required = false)  Optional<List<String>> status,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    final Pageable pageable = PageRequest.of(page, limit, sort);
    return workflowService.query(pageable, labels, status);
  }

  @PostMapping(value = "/")
  @Operation(summary = "Create a new workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> createWorkflow(@RequestBody Workflow workflow) {
    return workflowService.create(workflow);
  }

  //TODO
  @PostMapping(value = "/{workflowId}")
  @Operation(summary = "Update a workflow and create a new workflow revision")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> createWorkflowRevision(@RequestBody Workflow workflow) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  //TODO
  @DeleteMapping(value = "/{workflowId}")
  @Operation(summary = "Archive a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> archiveWorkflow(@RequestBody Workflow workflow) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
