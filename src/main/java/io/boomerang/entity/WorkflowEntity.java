package io.boomerang.entity;

import java.util.List;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.entity.model.WorkflowStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows')}")
public class WorkflowEntity {
  @Id
  private String id;

  private String name;

  private String description;

  private List<KeyValuePair> labels;

  private WorkflowStatus status;

//  private List<WorkflowTrigger> triggers;

//  private WorkflowScope scope;

//  private WorkflowResources resources;

//private WorkflowInputs inputs;

  public String getDescription() {
    return description;
  }
  
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }

}
