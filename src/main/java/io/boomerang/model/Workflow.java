package io.boomerang.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.data.entity.WorkflowEntity;
import io.boomerang.data.model.TaskExecution;

/*
 * Workflow Model joining Workflow Entity and Workflow Revision Entity 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow extends WorkflowEntity {

  private Integer version;
  
  private String versionId;
  
  private List<TaskExecution> tasks;

  private List<AbstractConfigurationProperty> params;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public List<TaskExecution> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskExecution> tasks) {
    this.tasks = tasks;
  }

  public List<AbstractConfigurationProperty> getParams() {
    return params;
  }

  public void setParams(List<AbstractConfigurationProperty> params) {
    this.params = params;
  }

  public String getVersionId() {
    return versionId;
  }

  public void setVersionId(String versionId) {
    this.versionId = versionId;
  }  
}
