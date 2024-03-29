package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.model.WorkflowTemplate;

public interface WorkflowTemplateService {

  WorkflowTemplate get(String name, Optional<Integer> version, boolean withTasks);

  Page<WorkflowTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryNames);

  WorkflowTemplate create(WorkflowTemplate request);

  WorkflowTemplate apply(WorkflowTemplate workflow, boolean replace);

  void delete(String name);
}
