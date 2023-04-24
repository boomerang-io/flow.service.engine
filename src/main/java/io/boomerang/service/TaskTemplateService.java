package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.TaskTemplate;

public interface TaskTemplateService {

  TaskTemplate get(String id, Optional<Integer> version);

  ResponseEntity<TaskTemplate> create(TaskTemplate taskTemplate);

  Page<TaskTemplate> query(Pageable pageable, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds);

  ResponseEntity<TaskTemplate> apply(TaskTemplate taskTemplate, boolean replace);

  void disable(String name);

  void enable(String name);
}
