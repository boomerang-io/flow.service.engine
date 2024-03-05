package io.boomerang.aspect;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.boomerang.audit.AuditInterceptor;
import io.boomerang.audit.AuditType;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.enums.RunStatus;
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
  
  @Autowired
  AuditInterceptor auditInterceptor;

  @Before("execution(* io.boomerang.data.repository.WorkflowRunRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void saveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    LOGGER.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (entityToBeSaved instanceof WorkflowRunEntity) {
      workflowRunEntityToBeUpdated((WorkflowRunEntity) entityToBeSaved);
    }
  }
  
  @AfterReturning(pointcut="execution(* io.boomerang.data.repository.WorkflowRunRepository.save(..)) && args(request)", returning="entity")
  public void saveInvoked(JoinPoint thisJoinPoint, WorkflowRunEntity request, WorkflowRunEntity entity) {

    LOGGER.info("Intercepted save action on entity {} from {}", request,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (request.getStatus().equals(RunStatus.notstarted)) {
      auditInterceptor.createWfRunLog(entity.getId(), entity.getWorkflowRef(), Optional.empty());
    } else {
      LOGGER.info("Status Label: {}, Audit Type: {}", entity.getStatus().getStatus(), AuditType.valueOfLabel(entity.getStatus().getStatus()));
      auditInterceptor.updateWfRunLog(AuditType.valueOfLabel(entity.getStatus().getStatus()), entity.getId(), Optional.of(Map.of("duration", String.valueOf(entity.getDuration()))));
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