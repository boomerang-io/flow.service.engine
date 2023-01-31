package io.boomerang.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
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
import io.boomerang.model.events.TaskRunStatusEvent;
import io.boomerang.model.events.WorkflowRunStatusEvent;
import io.boomerang.util.EventFactory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

@Service
public class EventSinkServiceImpl implements EventSinkService {
  private static final Logger LOGGER = LogManager.getLogger();

  protected static final String LABEL_KEY_INITIATOR_ID = "initiatorId";

  protected static final String LABEL_KEY_INITIATOR_CONTEXT = "initiatorContext";

  @Value("${flow.events.sink-urls}")
  private String sinkUrls;

  @Value("${flow.events.sink-enabled}")
  private boolean sinkEnabled;

  @Autowired
  // @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Override
  public Future<Boolean> publishStatusCloudEvent(TaskRunEntity taskRunEntity) {
    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;

      try {// Create status update CloudEvent from task execution
        TaskRunStatusEvent statusEvent = EventFactory.buildStatusUpdateEvent(taskRunEntity);

        // Extract initiatorId and initiatorContext
        String initiatorId = "";
        String initiatorContext = "";
        if (taskRunEntity.getLabels() != null && !taskRunEntity.getLabels().isEmpty()) {
          initiatorId = taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID) != null
              ? taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID)
              : "";
          if (taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT) != null) {
            initiatorContext = taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT) != null
                ? taskRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT)
                : "";
          }
        }
        statusEvent.setInitiatorId(initiatorId);
        statusEvent.setInitiatorContext(initiatorContext);

        httpSink(statusEvent.toCloudEvent());
        isSuccess = Boolean.TRUE;
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
        WorkflowRunStatusEvent statusEvent = EventFactory.buildStatusUpdateEvent(workflowRunEntity);

        // Extract initiatorId and initiatorContext
        String initiatorId = "";
        String initiatorContext = "";
        if (workflowRunEntity.getLabels() != null && !workflowRunEntity.getLabels().isEmpty()) {
          initiatorId = workflowRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID) != null
              ? workflowRunEntity.getLabels().get(LABEL_KEY_INITIATOR_ID)
              : "";
          if (workflowRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT) != null) {
            initiatorContext =
                workflowRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT) != null
                    ? workflowRunEntity.getLabels().get(LABEL_KEY_INITIATOR_CONTEXT)
                    : "";
          }
        }
        statusEvent.setInitiatorId(initiatorId);
        statusEvent.setInitiatorContext(initiatorContext);

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
    if (sinkEnabled && sinkUrls != null && !sinkUrls.isEmpty()) {
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/cloudevents+json");

      byte[] serialized = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
          .serialize(cloudEvent);

      final HttpEntity<byte[]> req = new HttpEntity<>(serialized, headers);

      String[] sinkUrlList = sinkUrls.split(",");
      for (String sinkUrl : sinkUrlList) {
        LOGGER.debug("httpSink() - URL: " + sinkUrl);
        ResponseEntity<String> responseEntity =
            restTemplate.exchange(sinkUrl, HttpMethod.POST, req, String.class);
        LOGGER.debug("httpSink() - Status Code: " + responseEntity.getStatusCode());
        if (responseEntity.getBody() != null) {
          LOGGER.debug("httpSink() - Body: " + responseEntity.getBody().toString());
        }
      }
    }
  }

}
