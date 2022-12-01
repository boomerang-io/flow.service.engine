package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.model.TaskTemplate;

public interface TaskTemplateService {

  TaskTemplate get(String id, Optional<Integer> version);

  ResponseEntity<TaskTemplate> create(TaskTemplate taskTemplate);

  Page<TaskTemplateEntity> query(Pageable pageable, Optional<List<String>> labels,
      Optional<List<String>> status);

  ResponseEntity<TaskTemplate> apply(TaskTemplate taskTemplate, boolean replace);

//  TektonTask getTaskTemplateYamlWithId(String id);
//  
//  List<FlowTaskTemplate> getAllTaskTemplates(TemplateScope scope, String teamId);
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
//  FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask,TemplateScope scope, String teamId);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask, Integer revision, String comment);
//
//  List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId);
//
//  FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask);
}
