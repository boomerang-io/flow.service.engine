package io.boomerang.aspect;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.service.EventSinkService;

@Aspect
@Component
@ConditionalOnProperty(name="flow.events.sink.enabled", havingValue="true", matchIfMissing = false)
public class TaskRunEntityUpdateInterceptor {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  TaskRunRepository taskRunRepository;
  
  @Autowired
  EventSinkService eventSinkService;

  @Before("execution(* io.boomerang.data.repository.TaskRunRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void saveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    LOGGER.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());
    if (entityToBeSaved instanceof TaskRunEntity) {
      taskRunEntityToBeUpdated((TaskRunEntity) entityToBeSaved);
    }
  }

  private void taskRunEntityToBeUpdated(TaskRunEntity newEntity) {
    // Check if WorkflowRun and Workflow IDs are not empty
    if (StringUtils.isNotBlank(newEntity.getWorkflowRunRef())
        && StringUtils.isNotBlank(newEntity.getId())) {
      
      taskRunRepository.findById(newEntity.getId()).ifPresent(oldEntity -> {
        // Retrieve old entity and compare the statuses
        if (oldEntity.getStatus() != newEntity.getStatus()) {

          // Status has changed, publish status update CloudEvent
          //TODO: separate out phase and status events
          eventSinkService.publishStatusCloudEvent(newEntity);
          LOGGER.info("TaskRun Status / Phase has changed [Status: " + oldEntity.getStatus() + ", Phase: " + oldEntity.getPhase() + "] -> [Status: " + newEntity.getStatus() + ", Phase: " + newEntity.getPhase() + "].");
        }
      });
    }
  }
}