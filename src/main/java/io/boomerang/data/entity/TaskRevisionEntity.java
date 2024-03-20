 package io.boomerang.data.entity;

import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.Task;
import io.boomerang.model.TaskSpec;

/*
 * The versioned elements of a task
 * 
 * Ref: https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/document-references.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_revisions')}")
public class TaskRevisionEntity {

  @Id
  private String id;
  private String parentRef;
  private String displayName;
  private String description;
  private String category;
  private String icon;
  private Integer version;
  private ChangeLog changelog;
  private TaskSpec spec = new TaskSpec();
  private List<AbstractParam> config;

  public TaskRevisionEntity() {
    // Do nothing
  }

  public TaskRevisionEntity(Task task) {
    BeanUtils.copyProperties(task, this, "id", "parentRef");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getParentRef() {
    return parentRef;
  }
  
  public void setParentRef(String parentRef) {
    this.parentRef = parentRef;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public TaskSpec getSpec() {
    return spec;
  }

  public void setSpec(TaskSpec spec) {
    this.spec = spec;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public List<AbstractParam> getConfig() {
    return config;
  }

  public void setConfig(List<AbstractParam> config) {
    this.config = config;
  }
}
