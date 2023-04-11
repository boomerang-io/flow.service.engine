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
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.service.EventSinkService;

@Aspect
@Component
@ConditionalOnProperty(name="flow.events.sink.enabled", havingValue="true", matchIfMissing = false)
public class WorkflowRunEntityUpdateInterceptor {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  WorkflowRunRepository workflowRunRepository;
  
  @Autowired
  EventSinkService eventSinkService;

  @Before("execution(* io.boomerang.data.repository.WorkflowRunRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void beforeSaveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    LOGGER.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (entityToBeSaved instanceof WorkflowRunEntity) {
      workflowRunEntityToBeUpdated((WorkflowRunEntity) entityToBeSaved);
    }
  }

  private void workflowRunEntityToBeUpdated(WorkflowRunEntity newEntity) {

    // Check if activity and workflow IDs are not empty
    if (StringUtils.isNotBlank(newEntity.getWorkflowRef())
        && StringUtils.isNotBlank(newEntity.getId())) {

      // Retrieve old entity and compare the statuses
      workflowRunRepository.findById(newEntity.getId()).ifPresent(oldEntity -> {
        if (oldEntity.getStatus() != newEntity.getStatus() || oldEntity.getPhase() != newEntity.getPhase()) {

          // Status has changed, publish status update CloudEvent
//          eventingService.publishStatusCloudEvent(newActivityEntity);
          //TODO: separate out phase and status events
          eventSinkService.publishStatusCloudEvent(newEntity);

          LOGGER.info("WorkflowRun Status / Phase has changed [Status: " + oldEntity.getStatus() + ", Phase: " + oldEntity.getPhase() + "] -> [Status: " + newEntity.getStatus() + ", Phase: " + newEntity.getPhase() + "].");
        }
      });
    }
  }
}