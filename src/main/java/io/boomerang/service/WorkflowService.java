package io.boomerang.service;

import io.boomerang.entity.WorkflowEntity;

public interface WorkflowService {

//  void deleteWorkflow(String workFlowid);
//
  WorkflowEntity getWorkflow(String workflowId);
//
//  List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId);
//  
//  /**
//   * Get workflows for a list of teams
//   * @param flowTeamIds
//   * @return Map<teamId, List<WorkflowSummary>>
//   */
//  Map<String, List<WorkflowSummary>> getWorkflowsForTeams(List<String> flowTeamIds);
//
//  WorkflowSummary saveWorkflow(WorkflowEntity flowWorkflowEntity);
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
//  public List<WorkflowShortSummary> getWorkflowShortSummaryList();
//
//  ResponseEntity<HttpStatus> validateWorkflowToken(String id, GenerateTokenResponse tokenPayload);
//
//  void deleteToken(String id, String label);
//
//  List<WorkflowSummary> getSystemWorkflows();
//  
//  UserWorkflowSummary getUserWorkflows();
//
//  List<WorkflowShortSummary> getSystemWorkflowShortSummaryList();
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
