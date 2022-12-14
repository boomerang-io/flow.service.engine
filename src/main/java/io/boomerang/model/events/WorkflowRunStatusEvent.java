package io.boomerang.model.events;

import java.io.IOException;
import java.time.ZoneOffset;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class WorkflowRunStatusEvent extends Event {

  private WorkflowRunEntity workflowRunEntity;

  @Override
  public CloudEvent toCloudEvent() throws IOException {

//    String json = new Gson().toJson(workflowRunEntity);
    
//    JsonCloudEventData jsonData = new JsonCloudEventData(null);
    
//    CloudEventData pojoData = PojoCloudEventData.wrap(workflowRunEntity, workflowRunEntity::toBytes);
    
    ObjectMapper mapper = new ObjectMapper();    
    JsonNode node = mapper.convertValue(workflowRunEntity, JsonNode.class);

    // @formatter:off
    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(getType().getCloudEventType())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(MediaType.APPLICATION_JSON_VALUE, node.toString().getBytes());
    // @formatter:on

    return cloudEventBuilder.build();
  }

  public WorkflowRunEntity getWorkflowRunEntity() {
    return workflowRunEntity;
  }

  public void setWorkflowRunEntity(WorkflowRunEntity workflowRunEntity) {
    this.workflowRunEntity = workflowRunEntity;
  }
}