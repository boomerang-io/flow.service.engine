
package io.boomerang.model.projectstormv5;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.model.TaskRunResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"inputs", "nodeId", "taskId"})
public class ConfigNodes {

  @JsonProperty("type")
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("inputs")
  private Map<String, String> inputs;
  @JsonProperty("nodeId")
  private String nodeId;
  @JsonProperty("taskId")
  private String taskId;

  private Integer taskVersion;

  @JsonProperty("outputs")
  private List<TaskRunResult> outputs = new LinkedList<>();

  public List<TaskRunResult> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<TaskRunResult> outputs) {
    this.outputs = outputs;
  }

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("inputs")
  public Map<String, String> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  @JsonProperty("nodeId")
  public String getNodeId() {
    return nodeId;
  }

  @JsonProperty("nodeId")
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @JsonProperty("taskVersion")
  public Integer getTaskVersion() {
    return taskVersion;
  }

  @JsonProperty("taskVersion")
  public void setTaskVersion(Integer taskVersion) {
    this.taskVersion = taskVersion;
  }

}
