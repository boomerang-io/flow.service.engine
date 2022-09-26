package io.boomerang.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.entity.model.DAG;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_revisions')}")
public class WorkflowRevisionEntity {

  @Id
  private String id;

  private long version;

  private String workflowId;

  private DAG dag;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
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
}
