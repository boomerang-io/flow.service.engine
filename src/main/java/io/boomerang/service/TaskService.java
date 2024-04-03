package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.model.ChangeLogVersion;
import io.boomerang.model.WorkflowTask;
import io.boomerang.model.Task;

public interface TaskService {

  Task get(String id, Optional<Integer> version);

  Task create(Task task);

  Task apply(Task task, boolean replace);

  Task retrieveAndValidateTask(WorkflowTask wfTask);

  List<ChangeLogVersion> changelog(String id);

  void delete(String id);

  Page<Task> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames,
      Optional<List<String>> queryIds);
}
