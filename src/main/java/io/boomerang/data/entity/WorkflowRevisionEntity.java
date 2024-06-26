package io.boomerang.data.entity;

import java.util.LinkedList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.ParamSpec;
import io.boomerang.model.WorkflowTask;
import io.boomerang.model.WorkflowWorkspace;

/*
 * Workflow Revision Entity stores the detail for each version of the workflow in conjunction with Workflow Entity
 * 
 * A number of these elements are relied on by the Workflow model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_revisions')}")
public class WorkflowRevisionEntity {
  @Id
  private String id;

  private Integer version;

  private String workflowRef;

  private List<WorkflowTask> tasks = new LinkedList<>();

  private ChangeLog changelog;

  private String markdown;

  private List<ParamSpec> params;
  
  private List<WorkflowWorkspace> workspaces;
  
  private Long timeout;
  
  private Long retries;
  
  private List<AbstractParam> config;
  
  @Override
  public String toString() {
    return "WorkflowRevisionEntity [id=" + id + ", version=" + version + ", workflowRef="
        + workflowRef + ", tasks=" + tasks + ", changelog=" + changelog + ", markdown=" + markdown
        + ", params=" + params + ", workspaces=" + workspaces + ", config=" + config + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowId) {
    this.workflowRef = workflowId;
  }

  public List<WorkflowTask> getTasks() {
    return tasks;
  }

  public void setTasks(List<WorkflowTask> tasks) {
    this.tasks = tasks;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }

  public List<ParamSpec> getParams() {
    return params;
  }

  public void setParams(List<ParamSpec> params) {
    this.params = params;
  }

  public List<WorkflowWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<WorkflowWorkspace> workspaces) {
    this.workspaces = workspaces;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  public Long getRetries() {
    return retries;
  }

  public void setRetries(Long retries) {
    this.retries = retries;
  } 

  public List<AbstractParam> getConfig() {
    return config;
  }

  public void setConfig(List<AbstractParam> config) {
    this.config = config;
  }
}
