package io.boomerang.model.events;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.boomerang.model.RunError;
import io.boomerang.model.RunParam;
import io.boomerang.model.RunResult;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.enums.TaskType;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class StatusUpdateEvent extends Event {

  private String name;
  private String taskRunRef;
  private TaskType taskType;
  private RunStatus status; 
  private RunPhase phase;
  private List<RunParam> params = new LinkedList<>();
  private List<RunResult> results = new LinkedList<>();
  private String workflowRef;
  private String workflowRunRef;
  private String initiatorContext;
  private RunError error;
  private Map<String, String> additionalData;

  @Override
  public CloudEvent toCloudEvent() throws IOException {

    // Configure and create Gson object
    Gson gson = new GsonBuilder().create();

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("name", name);
    jsonData.addProperty("taskRunRef", taskRunRef);
    jsonData.addProperty("workflowRef", workflowRef);
    jsonData.addProperty("workflowRunRef", workflowRunRef);
    jsonData.addProperty("status", status.toString());
    jsonData.addProperty("phase", phase.toString());
    jsonData.addProperty("taskType", taskType.toString());
    jsonData.add("parameters", gson.toJsonTree(params));
    jsonData.add("results", gson.toJsonTree(results));

    // Add error data to JSON data
    if (error != null) {
      jsonData.add("error", gson.toJsonTree(error));
    }

    // Add additional data to JSON data
    if (additionalData != null && !additionalData.isEmpty()) {
      additionalData.entrySet()
          .forEach(entry -> jsonData.addProperty(entry.getKey(), entry.getValue()));
    }

    // @formatter:off
    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(getType().getCloudEventType())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(MediaType.APPLICATION_JSON.toString(), jsonData.toString().getBytes());
    // @formatter:on

    if (Strings.isNotEmpty(initiatorContext)) {
      cloudEventBuilder =
          cloudEventBuilder.withExtension(EXTENSION_ATTRIBUTE_CONTEXT, initiatorContext);
    }

    return cloudEventBuilder.build();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public RunPhase getPhase() {
    return phase;
  }

  public void setPhase(RunPhase phase) {
    this.phase = phase;
  }

  public List<RunParam> getParams() {
    return params;
  }

  public void setParams(List<RunParam> params) {
    this.params = params;
  }

  public List<RunResult> getResults() {
    return results;
  }

  public void setResults(List<RunResult> results) {
    this.results = results;
  }

  public String getTaskRunRef() {
    return taskRunRef;
  }

  public void setTaskRunRef(String taskRunRef) {
    this.taskRunRef = taskRunRef;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public String getWorkflowRunRef() {
    return workflowRunRef;
  }

  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }

  public String getInitiatorContext() {
    return initiatorContext;
  }

  public void setInitiatorContext(String initiatorContext) {
    this.initiatorContext = initiatorContext;
  }

  public RunError getError() {
    return error;
  }

  public void setError(RunError runError) {
    this.error = runError;
  }

  public Map<String, String> getAdditionalData() {
    return additionalData;
  }

  public void setAdditionalData(Map<String, String> additionalData) {
    this.additionalData = additionalData;
  }
}