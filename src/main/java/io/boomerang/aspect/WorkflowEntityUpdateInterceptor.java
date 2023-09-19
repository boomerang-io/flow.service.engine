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
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.repository.WorkflowRepository;
import io.boomerang.service.EventSinkService;

@Aspect
@Component
@ConditionalOnProperty(name="flow.events.sink.enabled", havingValue="true", matchIfMissing = false)
public class WorkflowEntityUpdateInterceptor {
  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  WorkflowRepository workflowRepository;
  
  @Autowired
  EventSinkService eventSinkService;

  @Before("execution(* io.boomerang.data.repository.WorkflowRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void saveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    LOGGER.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (entityToBeSaved instanceof WorkflowEntity) {
      workflowEntityToBeUpdated((WorkflowEntity) entityToBeSaved);
    }
  }

  private void workflowEntityToBeUpdated(WorkflowEntity newEntity) {

    // Check if activity and workflow IDs are not empty
    if (StringUtils.isNotBlank(newEntity.getId())) {

      // Retrieve old entity and compare the statuses
      workflowRepository.findById(newEntity.getId()).ifPresent(oldEntity -> {
        if (oldEntity.getStatus() != newEntity.getStatus()) {

          // Status has changed, publish status update CloudEvent
//          eventingService.publishStatusCloudEvent(newActivityEntity);
          //TODO: separate out phase and status events
          eventSinkService.publishStatusCloudEvent(newEntity);

          LOGGER.info("Workflow Status has changed [Status: " + oldEntity.getStatus() + "] -> [Status: " + newEntity.getStatus() + "].");
        }
      });
    }
  }
}