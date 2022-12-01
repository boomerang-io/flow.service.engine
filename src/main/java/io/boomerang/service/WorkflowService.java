package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.model.Workflow;

public interface WorkflowService {

  ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version);

  ResponseEntity<Workflow> create(Workflow workflow);

  Page<WorkflowEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status);

  ResponseEntity<Workflow> apply(Workflow workflow, Boolean replace);
//
//  WorkflowSummary updateWorkflow(WorkflowSummary summary);
//
//  WorkflowSummary updateWorkflowProperties(String workflowId, List<WorkflowProperty> properties);
//
//  GenerateTokenResponse generateTriggerToken(String id, String label);
//
//  ResponseEntity<InputStreamResource> exportWorkflow(String workFlowId);
//
//  void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId, WorkflowScope scope);
//  
//  boolean canExecuteWorkflowForQuotas(String teamId);
//
//  boolean canExecuteWorkflow(String workFlowId, Optional<String> trigger);
//
//  ResponseEntity<HttpStatus> validateWorkflowToken(String id, GenerateTokenResponse tokenPayload);
//
//  void deleteToken(String id, String label);
//
//  List<String> getWorkflowParameters(String workFlowId);
//
//  List<String> getWorkflowParameters(String workflowId, FlowWorkflowRevision workflowSummaryEntity);
//
//  WorkflowSummary duplicateWorkflow(String id, DuplicateRequest duplicateRequest);
//
//  boolean canExecuteWorkflowForQuotasForUser(String workflowId);
//
//  List<TemplateWorkflowSummary> getTemplateWorkflows();
//
//  UserWorkflowSummary getUserWorkflows(String userId);
//
//  boolean canExecuteTeamWorkflow(String teamId);
//
//  ResponseEntity<WFETriggerResponse> getRevisionProperties(String workflowId, long workflowVersion, String taskId,
//      String propertyKey);
}
