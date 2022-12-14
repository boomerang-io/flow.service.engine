package io.boomerang.util;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.enums.EventType;
import io.boomerang.model.events.StatusUpdateEvent;
import io.boomerang.model.events.WorkflowRunStatusEvent;

public class EventFactory {

  private static final String EVENT_SOURCE_URI = "/apis/v1/events";

  private EventFactory() {}

//  public static Event buildFromCloudEvent(CloudEvent cloudEvent)
//      throws InvalidPropertiesFormatException {
//    EventType eventType = EventType.valueOfCloudEventType(cloudEvent.getType());
//    InvalidPropertiesFormatException invalidCloudEventType = new InvalidPropertiesFormatException(
//        MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
//
//    if (eventType == null) {
//      throw invalidCloudEventType;
//    }
//
//    switch (eventType) {
//      case TRIGGER:
//        return EventTrigger.fromCloudEvent(cloudEvent);
//      case WFE:
//        return EventWFE.fromCloudEvent(cloudEvent);
//      case CANCEL:
//        return EventCancel.fromCloudEvent(cloudEvent);
//      default:
//        throw invalidCloudEventType;
//    }
//  }

  public static WorkflowRunStatusEvent buildStatusUpdateEvent(WorkflowRunEntity wfRunEntity) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/workflow/run/{0}/status/{1}",
        wfRunEntity.getId(),
        wfRunEntity.getStatus().toString().toLowerCase());
    // @formatter:off

    // Create workflow status update event
    WorkflowRunStatusEvent statusEvent = new WorkflowRunStatusEvent();
    statusEvent.setId(UUID.randomUUID().toString());
    statusEvent.setSource(URI.create(EVENT_SOURCE_URI));
    statusEvent.setSubject(eventSubject);
    statusEvent.setDate(new Date());
    statusEvent.setType(EventType.WORKFLOW_STATUS_UPDATE);
    statusEvent.setWorkflowRunEntity(wfRunEntity);

    return statusEvent;
  }

  public static StatusUpdateEvent buildStatusUpdateEvent(
      TaskRunEntity taskRunEntity, Map<String, String> additionalData) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/task/run/{0}/status/{1}",
        taskRunEntity.getId(),
        taskRunEntity.getStatus().toString().toLowerCase());
    // @formatter:on

    // Create task status update event
    StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent();
    statusUpdateEvent.setId(UUID.randomUUID().toString());
    statusUpdateEvent.setSource(URI.create(EVENT_SOURCE_URI));
    statusUpdateEvent.setSubject(eventSubject);
    statusUpdateEvent.setDate(new Date());
    statusUpdateEvent.setType(EventType.TASK_STATUS_UPDATE);
    statusUpdateEvent.setName(taskRunEntity.getName());
    statusUpdateEvent.setTaskRunRef(taskRunEntity.getId());
    statusUpdateEvent.setWorkflowRef(taskRunEntity.getWorkflowRef());
    statusUpdateEvent.setWorkflowRunRef(taskRunEntity.getWorkflowRunRef());
    statusUpdateEvent.setStatus(taskRunEntity.getStatus());
    statusUpdateEvent.setPhase(taskRunEntity.getPhase());
    statusUpdateEvent.setTaskType(taskRunEntity.getType());
    statusUpdateEvent.setParams(taskRunEntity.getParams());
    statusUpdateEvent.setResults(taskRunEntity.getResults());
    statusUpdateEvent.setError(taskRunEntity.getError());
    statusUpdateEvent.setAdditionalData(additionalData);

    return statusUpdateEvent;
  }
}