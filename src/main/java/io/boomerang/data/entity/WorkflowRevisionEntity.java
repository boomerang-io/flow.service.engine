package io.boomerang.data.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.DAG;
import io.boomerang.model.AbstractConfigurationProperty;
import io.boomerang.model.ChangeLog;
import io.boomerang.model.projectstormv5.RestConfig;

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

  private String workflowId;

  private DAG dag;

  private ChangeLog changelog;

  private String markdown;
  
  //TODO: merge into the DAG
  private RestConfig config;
  
//  private WorkflowWorkspaces workspaces;

  private List<AbstractConfigurationProperty> params;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public DAG getDag() {
    return dag;
  }

  public void setDag(DAG dag) {
    this.dag = dag;
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

  public RestConfig getConfig() {
    return config;
  }

  public void setConfig(RestConfig config) {
    this.config = config;
  }

  public List<AbstractConfigurationProperty> getParams() {
    return params;
  }

  public void setParams(List<AbstractConfigurationProperty> params) {
    this.params = params;
  } 
}
