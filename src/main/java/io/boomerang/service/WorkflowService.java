package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.Workflow;
import io.boomerang.model.WorkflowCount;
import io.boomerang.model.WorkflowRunCount;

public interface WorkflowService {

  ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks);

  ResponseEntity<Workflow> create(Workflow workflow, boolean useId);

  Page<Workflow> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds);

  ResponseEntity<Workflow> apply(Workflow workflow, Boolean replace);

  void delete(String workflowId);

  ResponseEntity<List<ChangeLogVersion>> changelog(String workflowId);

  ResponseEntity<WorkflowCount> count(Optional<Date> from, Optional<Date> to,
      Optional<List<String>> labels, Optional<List<String>> ids);

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
