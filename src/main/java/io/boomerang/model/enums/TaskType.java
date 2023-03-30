package io.boomerang.model.enums;

/*
 * TaskTypes map to TaskTemplates and also determine the logic gates for TaskExecution.
 * 
 * If new TaskTypes are added, additional logic is needed in TaskExecutionServiceImpl
 * 
 * If TaskTypes are altered, logic will need to be checked in TaskExecutionServiceImpl
 */
public enum TaskType {
  start, end, template, custom, generic, decision, approval, setwfproperty, manual, eventwait, acquirelock, releaselock, runworkflow, runscheduledworkflow, script, setwfstatus, sleep
}
