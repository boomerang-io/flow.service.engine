package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.model.WorkflowTask;
import io.boomerang.model.TaskTemplate;

public interface TaskTemplateService {

  TaskTemplate get(String id, Optional<Integer> version);

  ResponseEntity<TaskTemplate> create(TaskTemplate taskTemplate);

  Page<TaskTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> labels, Optional<List<String>> status, Optional<List<String>> queryNames);

  ResponseEntity<TaskTemplate> apply(TaskTemplate taskTemplate, boolean replace);

  TaskTemplate disable(String name);

  TaskTemplate enable(String name);

  Optional<TaskTemplateEntity> retrieveAndValidateTaskTemplate(WorkflowTask wfRevisionTask);
}
