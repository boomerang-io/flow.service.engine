package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.TaskTemplateRevision;
import io.boomerang.data.model.TaskTemplateStatus;
import io.boomerang.model.enums.TaskTemplateScope;
import io.boomerang.model.enums.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_templates')}")
public class TaskTemplateEntity {

  @Id
  private String id;
  private String name;
  
  @JsonIgnore
  private Integer currentVersion;
  private String description;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private Date creationDate;
  private Date lastModified;
  private String category;
  private TaskType type;
  
  @JsonIgnore
  private List<TaskTemplateRevision> revisions;
  private TaskTemplateStatus status;
  private String icon;
  private boolean verified;
  
  //TODO: change this to a relationship
  private TaskTemplateScope scope = TaskTemplateScope.global;

  public TaskTemplateEntity() {
    // Do nothing
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getCurrentVersion() {
    return currentVersion;
  }

  public void setCurrentVersion(Integer currentVersion) {
    this.currentVersion = currentVersion;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
  
  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public List<TaskTemplateRevision> getRevisions() {
    return revisions;
  }

  public void setRevisions(List<TaskTemplateRevision> revisions) {
    this.revisions = revisions;
  }

  public TaskTemplateStatus getStatus() {
    return status;
  }

  public void setStatus(TaskTemplateStatus status) {
    this.status = status;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public boolean isVerified() {
    return verified;
  }

  public void setVerified(boolean verified) {
    this.verified = verified;
  }

  public TaskTemplateScope getScope() {
    return scope;
  }

  public void setScope(TaskTemplateScope scope) {
    this.scope = scope;
  }
}
