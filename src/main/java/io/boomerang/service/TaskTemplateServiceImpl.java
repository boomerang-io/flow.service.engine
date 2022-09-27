package io.boomerang.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.repository.TaskTemplateRepository;

@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

  @Autowired
  private TaskTemplateRepository taskTemplateRepository;

//  @Autowired
//  private WorkflowService workflowService;

  @Override
  public TaskTemplateEntity getTaskTemplateWithId(String id) {
    return taskTemplateRepository.findById(id).orElse(null);
  }

//  @Override
//  public List<FlowTaskTemplate> getAllTaskTemplates(TaskTemplateScope scope, String teamId) {
//    List<FlowTaskTemplate> templates = new LinkedList<>();
//
//    if (scope == TaskTemplateScope.global || scope == null) {
//      templates = flowTaskTemplateService.getAllGlobalTasks().stream().map(FlowTaskTemplate::new)
//          .collect(Collectors.toList());
//    } else if (scope == TaskTemplateScope.team) {
//      templates = flowTaskTemplateService.getTaskTemplatesforTeamId(teamId).stream()
//          .map(FlowTaskTemplate::new).collect(Collectors.toList());
//    } else if (scope == TaskTemplateScope.system) {
//      templates = flowTaskTemplateService.getAllSystemTasks().stream().map(FlowTaskTemplate::new)
//          .collect(Collectors.toList());
//    }
//
//    updateTemplateListUserNames(templates);
//    return templates;
//  }

//  private void updateTemplateListUserNames(List<FlowTaskTemplate> templates) {
//    for (FlowTaskTemplate template : templates) {
//      for (Revision revision : template.getRevisions()) {
//        if (revision.getChangelog() != null && revision.getChangelog().getUserId() != null) {
//          FlowUserEntity user =
//              userIdentityService.getUserByID(revision.getChangelog().getUserId());
//          if (revision.getChangelog() != null && user != null
//              && revision.getChangelog().getUserName() == null) {
//            revision.getChangelog().setUserName(user.getName());
//          }
//        }
//      }
//    }
//  }
//
//  @Override
//  public FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
//    FlowUserEntity user = userIdentityService.getCurrentUser();
//
//    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {
//
//      Date creationDate = new Date();
//
//      flowTaskTemplateEntity.setCreatedDate(creationDate);
//      flowTaskTemplateEntity.setLastModified(creationDate);
//      flowTaskTemplateEntity.setVerified(false);
//
//      updateChangeLog(flowTaskTemplateEntity);
//
//      return new FlowTaskTemplate(
//          flowTaskTemplateService.insertTaskTemplate(flowTaskTemplateEntity));
//
//    } else {
//      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//    }
//
//  }
//
//  @Override
//  public FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
//    FlowUserEntity user = userIdentityService.getCurrentUser();
//
//    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {
//
//      updateChangeLog(flowTaskTemplateEntity);
//
//      flowTaskTemplateEntity.setLastModified(new Date());
//      flowTaskTemplateEntity.setVerified(flowTaskTemplateService
//          .getTaskTemplateWithId(flowTaskTemplateEntity.getId()).isVerified());
//      return new FlowTaskTemplate(
//          flowTaskTemplateService.updateTaskTemplate(flowTaskTemplateEntity));
//
//    } else {
//      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//    }
//  }
//
//  @Override
//  public void deleteTaskTemplateWithId(String id) {
//    FlowUserEntity user = userIdentityService.getCurrentUser();
//
//    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {
//      flowTaskTemplateService.deleteTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));
//    } else {
//      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//    }
//  }
//
//  @Override
//  public void activateTaskTemplate(String id) {
//    flowTaskTemplateService.activateTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));
//
//  }
//
//  private void updateChangeLog(FlowTaskTemplate flowTaskTemplateEntity) {
//    List<Revision> revisions = flowTaskTemplateEntity.getRevisions();
//    final FlowUserEntity user = userIdentityService.getCurrentUser();
//
//    if (revisions != null) {
//      for (Revision revision : revisions) {
//        ChangeLog changelog = revision.getChangelog();
//        if (changelog != null && changelog.getUserId() == null) {
//          changelog.setUserId(user.getId());
//          changelog.setDate(new Date());
//          changelog.setUserName(user.getName());
//        }
//      }
//    }
//  }
//
//  @Override
//  public TektonTask getTaskTemplateYamlWithId(String id) {
//    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
//    return TektonConverter.convertFlowTaskToTekton(template, Optional.empty());
//  }
//
//  @Override
//  public TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber) {
//    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
//    return TektonConverter.convertFlowTaskToTekton(template, Optional.of(revisionNumber));
//  }
//
//  @Override
//  public FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask, TaskTemplateScope scope,
//      String teamId) {
//    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
//    template.setStatus(FlowTaskTemplateStatus.active);
//    template.setScope(scope);
//    template.setFlowTeamId(teamId);
//
//    flowTaskTemplateService.insertTaskTemplate(template);
//    return new FlowTaskTemplate(template);
//  }
//
//  @Override
//  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask) {
//    FlowTaskTemplateEntity tektonTemplate =
//        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
//    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);
//
//    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
//      dbTemplate.setName(tektonTemplate.getName());
//    }
//    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
//      dbTemplate.setCategory(tektonTemplate.getCategory());
//    }
//
//    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
//      dbTemplate.setDescription(tektonTemplate.getDescription());
//    }
//
//    List<Revision> revisions = tektonTemplate.getRevisions();
//    if (revisions.size() == 1) {
//      Revision revision = revisions.get(0);
//
//      final FlowUserEntity user = userIdentityService.getCurrentUser();
//      if (user != null) {
//        ChangeLog changelog = revision.getChangelog();
//        changelog.setUserId(user.getId());
//        changelog.setDate(new Date());
//      }
//
//
//      List<Revision> existingRevisions = dbTemplate.getRevisions();
//      int count = existingRevisions.size();
//      revision.setVersion(count + 1);
//      existingRevisions.add(revision);
//    }
//    dbTemplate.setLastModified(new Date());
//    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
//    return this.getTaskTemplateWithId(id);
//  }
//
//  @Override
//  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask,
//      Integer revisionId, String comment) {
//    FlowTaskTemplateEntity tektonTemplate =
//        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
//    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);
//
//    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
//      dbTemplate.setName(tektonTemplate.getName());
//    }
//    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
//      dbTemplate.setCategory(tektonTemplate.getCategory());
//    }
//
//    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
//      dbTemplate.setDescription(tektonTemplate.getDescription());
//    }
//
//    List<Revision> revisions = tektonTemplate.getRevisions();
//    if (revisions.size() == 1) {
//      Revision revision = revisions.get(0);
//      revision.setVersion(revisionId);
//
//      final FlowUserEntity user = userIdentityService.getCurrentUser();
//      if (user != null) {
//        ChangeLog changelog = revision.getChangelog();
//        changelog.setUserId(user.getId());
//        changelog.setDate(new Date());
//        changelog.setReason(comment);
//      }
//
//      List<Revision> existingRevisions = dbTemplate.getRevisions();
//
//      Revision oldRevision = existingRevisions.stream()
//          .filter(a -> a.getVersion().equals(revisionId)).findFirst().orElse(null);
//      if (oldRevision != null) {
//        existingRevisions.remove(oldRevision);
//
//      }
//      existingRevisions.add(revision);
//    }
//    dbTemplate.setLastModified(new Date());
//    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
//    return this.getTaskTemplateWithId(id);
//  }
//
//  @Override
//  public List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId) {
//    List<FlowTaskTemplate> templates = new LinkedList<>();
//
//    WorkflowSummary workflow = this.workflowService.getWorkflow(workflowId);
//    String flowTeamId = workflow.getFlowTeamId();
//    if (workflow.getScope() == WorkflowScope.team || workflow.getScope() == null) {
//      templates = flowTaskTemplateService.getAllTaskTemplatesforTeamId(flowTeamId).stream()
//          .map(FlowTaskTemplate::new).collect(Collectors.toList());
//    } else if (workflow.getScope() == WorkflowScope.system || workflow.getScope() == WorkflowScope.user || workflow.getScope() == WorkflowScope.template) {
//      templates = flowTaskTemplateService.getAllTaskTemplatesForSystem().stream()
//          .map(FlowTaskTemplate::new).collect(Collectors.toList());
//    }
//
//    return templates;
//  }
//
//  @Override
//  public FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask) {
//    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
//    template.setStatus(FlowTaskTemplateStatus.active);
//    return new FlowTaskTemplate(template);
//  }
}
