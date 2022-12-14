package io.boomerang.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.enums.TaskType;
import io.boomerang.model.events.StatusUpdateEvent;
import io.boomerang.model.events.WorkflowRunStatusEvent;
import io.boomerang.util.EventFactory;
import io.boomerang.util.ParameterUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

@Service
public class EventSinkServiceImpl implements EventSinkService {
  private static final Logger LOGGER = LogManager.getLogger();

  protected static final String LABEL_KEY_INITIATOR_ID = "initiatorId";

  protected static final String LABEL_KEY_INITIATOR_CONTEXT = "initiatorContext";

  @Value("${flow.events.sink.url}")
  private String sinkUrl;

  @Autowired
//  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Override
  public Future<Boolean> publishStatusCloudEvent(TaskRunEntity taskRunEntity) {
    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;

      try {
        Map<String, String> additionalData = new HashMap<>();
        // Retrieve WFE task topic if task is of type WFE
        if (TaskType.eventwait.equals(taskRunEntity.getType())) {
          String taskWFETopic = (String) ParameterUtil.getValue(taskRunEntity.getParams(), "topic");

          if (StringUtils.isNotBlank(taskWFETopic)) {
            additionalData.put("wfetopic", taskWFETopic);
          }
        }

        // Create status update CloudEvent from task execution
        StatusUpdateEvent eventStatusUpdate =
            EventFactory.buildStatusUpdateEvent(taskRunEntity, additionalData);
        String initiatorId = "";

        // Extract initiator ID and initiator context
        if (taskRunEntity.getLabels() != null && !taskRunEntity.getLabels().isEmpty()) {
          // String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";

          initiatorId = taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID) != null
              ? taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID)
              : "";
          if (taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT) != null) {
            eventStatusUpdate
                .setInitiatorContext(taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT));
          }
        }

        // Generate NATS message subject and publish cloud event
        // String natsSubject = generateNATSSubject(taskExecutionEntity, initiatorId);
        // String serializedCloudEvent = new String(eventFormatProvider
        // .resolveFormat(JsonFormat.CONTENT_TYPE).serialize(eventStatusUpdate.toCloudEvent()));
        //
        // outputEventsTunnel.publish(natsSubject, serializedCloudEvent);
        // isSuccess = Boolean.TRUE;
        //
        // logger.debug("Task with ID {} has changed its status to {}",
        // eventStatusUpdate.getTaskId(),
        // eventStatusUpdate.getStatus());

        httpSink(eventStatusUpdate.toCloudEvent());
        isSuccess = Boolean.TRUE;

        // } catch (IllegalStateException | IOException | JetStreamApiException e) {
        // LOGGER.error("An exception occurred while publishing the message to NATS server!", e);
        // } catch (StreamNotFoundException | SubjectMismatchException e) {
        // LOGGER.error("Stream is not configured properly!", e);
      } catch (Exception e) {
        LOGGER.fatal("A fatal error has occurred while publishing the message!", e);
      }
      return isSuccess;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

  @Override
  public Future<Boolean> publishStatusCloudEvent(WorkflowRunEntity workflowRunEntity) {
    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;

      try {
        // Create status update CloudEvent
        WorkflowRunStatusEvent statusEvent =
            EventFactory.buildStatusUpdateEvent(workflowRunEntity);

        httpSink(statusEvent.toCloudEvent());
        isSuccess = Boolean.TRUE;
      } catch (Exception e) {
        LOGGER.fatal("A fatal error has occurred while publishing the message!", e);
      }
      return isSuccess;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

  public void httpSink(CloudEvent cloudEvent) {
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/cloudevents+json");
    
    byte[]serialized = EventFormatProvider
        .getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE)
        .serialize(cloudEvent);

    final HttpEntity<byte[]> req = new HttpEntity<>(serialized, headers);

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(sinkUrl, HttpMethod.POST, req, String.class);

    LOGGER.debug("httpSink() - Status Code: " + responseEntity.getStatusCode());
    LOGGER.debug("httpSink() - Body: " + responseEntity.getBody().toString());
  }

}
