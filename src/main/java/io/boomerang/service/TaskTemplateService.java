package io.boomerang.service;

import io.boomerang.data.entity.TaskTemplateEntity;

public interface TaskTemplateService {

  TaskTemplateEntity getTaskTemplateWithId(String id);
//
//  TektonTask getTaskTemplateYamlWithId(String id);
//  
//  List<FlowTaskTemplate> getAllTaskTemplates(TaskTemplateScope scope, String teamId);
//
//  FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);
//
//  FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);
//
//  void deleteTaskTemplateWithId(String id);
//
//  void activateTaskTemplate(String id);
//
//  TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber);
//
//  FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask,TaskTemplateScope scope, String teamId);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask, Integer revision, String comment);
//
//  List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId);
//
//  FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask);
}
