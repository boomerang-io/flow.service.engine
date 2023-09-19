package io.boomerang.util;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.TaskRun;
import io.boomerang.model.Workflow;
import io.boomerang.model.WorkflowRun;
import io.boomerang.model.enums.EventType;
import io.boomerang.model.events.TaskRunStatusEvent;
import io.boomerang.model.events.WorkflowRunStatusEvent;
import io.boomerang.model.events.WorkflowStatusEvent;

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
    String eventSubject = MessageFormat.format("/workflowrun/{0}/status/{1}",
        wfRunEntity.getId(),
        wfRunEntity.getStatus().toString().toLowerCase());
    // @formatter:off

    // Create workflow status update event
    WorkflowRunStatusEvent statusEvent = new WorkflowRunStatusEvent();
    statusEvent.setId(UUID.randomUUID().toString());
    statusEvent.setSource(URI.create(EVENT_SOURCE_URI));
    statusEvent.setSubject(eventSubject);
    statusEvent.setDate(new Date());
    statusEvent.setType(EventType.WORKFLOWRUN_STATUS_UPDATE);
    statusEvent.setWorkflowRun(new WorkflowRun(wfRunEntity));

    return statusEvent;
  }

  public static TaskRunStatusEvent buildStatusUpdateEvent(
      TaskRunEntity taskRunEntity) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/taskrun/{0}/status/{1}",
        taskRunEntity.getId(),
        taskRunEntity.getStatus().toString().toLowerCase());
    // @formatter:on

    // Create task status update event
    TaskRunStatusEvent statusUpdateEvent = new TaskRunStatusEvent();
    statusUpdateEvent.setId(UUID.randomUUID().toString());
    statusUpdateEvent.setSource(URI.create(EVENT_SOURCE_URI));
    statusUpdateEvent.setSubject(eventSubject);
    statusUpdateEvent.setDate(new Date());
    statusUpdateEvent.setType(EventType.TASKRUN_STATUS_UPDATE);
    statusUpdateEvent.setTaskRun(new TaskRun(taskRunEntity));

    return statusUpdateEvent;
  }

  public static WorkflowStatusEvent buildStatusUpdateEvent(
      WorkflowEntity workflowEntity) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/workflow/{0}/status/{1}",
        workflowEntity.getId(),
        workflowEntity.getStatus().toString().toLowerCase());
    // @formatter:on

    // Create task status update event
    WorkflowStatusEvent statusUpdateEvent = new WorkflowStatusEvent();
    statusUpdateEvent.setId(UUID.randomUUID().toString());
    statusUpdateEvent.setSource(URI.create(EVENT_SOURCE_URI));
    statusUpdateEvent.setSubject(eventSubject);
    statusUpdateEvent.setDate(new Date());
    statusUpdateEvent.setType(EventType.TASKRUN_STATUS_UPDATE);
    statusUpdateEvent.setWorkflow(workflowEntity);

    return statusUpdateEvent;
  }
  

//TODO
//  public static GenericStatusEvent buildStatusUpdateEvent(Map<String, String> additionalData) {
//
//    // Event subject
//    // @formatter:off
//    String eventSubject = MessageFormat.format("/taskrun/{0}/status/{1}",
//        taskRunEntity.getId(),
//        taskRunEntity.getStatus().toString().toLowerCase());
//    // @formatter:on
//
//    // Create task status update event
//    TaskRunStatusEvent statusUpdateEvent = new TaskRunStatusEvent();
//    statusUpdateEvent.setId(UUID.randomUUID().toString());
//    statusUpdateEvent.setSource(URI.create(EVENT_SOURCE_URI));
//    statusUpdateEvent.setSubject(eventSubject);
//    statusUpdateEvent.setDate(new Date());
//    statusUpdateEvent.setType(EventType.TASKRUN_STATUS_UPDATE);
//    statusUpdateEvent.setAdditionalData(additionalData);
//
//    return statusUpdateEvent;
//  }
}