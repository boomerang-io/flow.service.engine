package io.boomerang.engine.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.boomerang.engine.model.enums.TaskType;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"name", "type", "taskRef", "taskVersion", "upgradesAvailable", "timeout", "retries", "dependencies" })
public class WorkflowTask {
  
  private String name;
  private TaskType type;
  private String taskRef;
  private Integer taskVersion;
  @Transient
  @JsonSerialize
  private Boolean upgradesAvailable = false;
  private Long timeout;
//  private long retries = -1;
  private List<WorkflowTaskDependency> dependencies = new LinkedList<>();;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  //Uses RunParam as the ParamSpec comes from the Task
  private List<RunParam> params = new LinkedList<>();
  //This is needed as some of our Tasks allow you to define Result Definitions on the fly
  private List<ResultSpec> results = new LinkedList<>();;
  //Optional - the default is that the workspace goes to all Tasks
  //Not supported by all integrations
  private List<TaskWorkspace> workspaces;
  private Map<String, Object> unknownFields = new HashMap<>();

  @JsonAnyGetter
  @JsonPropertyOrder(alphabetic = true)
  public Map<String, Object> otherFields() {
    return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, Object value) {
    unknownFields.put(name, value);
  }
}
