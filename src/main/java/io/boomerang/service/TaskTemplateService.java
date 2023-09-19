package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.Task;
import io.boomerang.model.TaskTemplate;

public interface TaskTemplateService {

  TaskTemplate get(String id, Optional<Integer> version);

  TaskTemplate create(TaskTemplate taskTemplate);

  Page<TaskTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> labels, Optional<List<String>> status, Optional<List<String>> queryNames);

  TaskTemplate apply(TaskTemplate taskTemplate, boolean replace);

  TaskTemplate retrieveAndValidateTaskTemplate(Task wfTask);

  List<ChangeLogVersion> changelog(String name);
}
