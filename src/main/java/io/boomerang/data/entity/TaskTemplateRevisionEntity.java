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
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.TaskTemplateSpec;

/*
 * The versioned elements of a task_template
 * 
 * Ref: https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/document-references.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_template_revisions')}")
public class TaskTemplateRevisionEntity {

  @Id
  private String id;
  private String parentRef;
  private String displayName;
  private String description;
  private String category;
  private String icon;
  private Integer version;
  private ChangeLog changelog;
  private TaskTemplateSpec spec = new TaskTemplateSpec();
  private List<AbstractParam> config;

  public TaskTemplateRevisionEntity() {
    // Do nothing
  }

  public TaskTemplateRevisionEntity(TaskTemplate taskTemplate) {
    BeanUtils.copyProperties(taskTemplate, this, "id", "parentRef");
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

  public TaskTemplateSpec getSpec() {
    return spec;
  }

  public void setSpec(TaskTemplateSpec spec) {
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
