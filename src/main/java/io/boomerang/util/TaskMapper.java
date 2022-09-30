package io.boomerang.util;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.model.TaskExecution;
import io.boomerang.data.model.WorkflowRevisionTask;
import io.boomerang.model.Task;
import io.boomerang.model.TaskExecutionRequest;

public class TaskMapper {

  /*
   * Converts a Task List a WorkflowRevisionTask List. For storage into MongoDB
   * 
   * @param tasks
   * 
   * @return list of WorkflowRevisionTasks
   */
  public static List<WorkflowRevisionTask> tasksToListOfRevisionTasks(List<Task> tasks) {
    List<WorkflowRevisionTask> wfRevisionTasks = new LinkedList<>();
    if (tasks != null) {
      for (Task t : tasks) {
        WorkflowRevisionTask wfRevisionTask = new WorkflowRevisionTask();
        BeanUtils.copyProperties(t, wfRevisionTask);
        wfRevisionTasks.add(wfRevisionTask);
      }
    }
    return wfRevisionTasks;
  }
  
  public static List<Task> revisionTasksToListOfTasks(List<WorkflowRevisionTask> wfRevisionTasks) {
    List<Task> tasks = new LinkedList<>();
    if (wfRevisionTasks != null) {
      for (WorkflowRevisionTask t : wfRevisionTasks) {
        Task task = new Task();
        BeanUtils.copyProperties(t, task);
        tasks.add(task);
      }
    }
    return tasks;
  }
  
  public static TaskExecution taskExecutionRequestToExecutionTask(TaskExecutionRequest executionTaskRequest) {
    TaskExecution taskExecution = new TaskExecution();
    if (executionTaskRequest != null) {
      taskExecution.setRunRef(executionTaskRequest.getTaskRunId());
      taskExecution.setWorkflowRunRef(executionTaskRequest.getWorkflowRunId());
      taskExecution.setParams(executionTaskRequest.getParams());
    }
    return taskExecution;
  }
}
