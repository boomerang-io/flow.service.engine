package io.boomerang.util;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.model.WorkflowTask;
import io.boomerang.model.Task;

public class TaskMapper {

  /*
   * Converts a Task List a WorkflowTask List. For storage into MongoDB
   * 
   * @param tasks
   * 
   * @return list of WorkflowTasks
   */
  public static List<WorkflowTask> tasksToListOfWorkflowTasks(List<Task> tasks) {
    List<WorkflowTask> wfTasks = new LinkedList<>();
    if (tasks != null) {
      for (Task t : tasks) {
        WorkflowTask wfRevisionTask = new WorkflowTask();
        BeanUtils.copyProperties(t, wfRevisionTask);
        wfTasks.add(wfRevisionTask);
      }
    }
    return wfTasks;
  }
  
  public static List<Task> workflowTasksToListOfTasks(List<WorkflowTask> wfTasks) {
    List<Task> tasks = new LinkedList<>();
    if (wfTasks != null) {
      for (WorkflowTask t : wfTasks) {
        Task task = new Task();
        BeanUtils.copyProperties(t, task);
        tasks.add(task);
      }
    }
    return tasks;
  }
}
