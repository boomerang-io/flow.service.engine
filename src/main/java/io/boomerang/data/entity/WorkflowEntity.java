package io.boomerang.data.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.WorkflowScope;
import io.boomerang.data.model.WorkflowStatus;
import io.boomerang.model.AbstractKeyValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows')}")
public class WorkflowEntity {
  @Id
  private String id;

  private String name;
  
  private String icon;

  private String description;

  private String shortDescription;

  private List<AbstractKeyValue> labels;

  private WorkflowStatus status;

//private List<WorkflowTrigger> triggers;

  private WorkflowScope scope;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public List<AbstractKeyValue> getLabels() {
    return labels;
  }

  public void setLabels(List<AbstractKeyValue> labels) {
    this.labels = labels;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }
}
