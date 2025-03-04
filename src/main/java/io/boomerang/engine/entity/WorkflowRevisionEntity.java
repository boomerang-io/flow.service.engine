package io.boomerang.engine.entity;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.engine.model.AbstractParam;
import io.boomerang.engine.model.ChangeLog;
import io.boomerang.engine.model.ParamSpec;
import io.boomerang.engine.model.WorkflowTask;
import io.boomerang.engine.model.WorkflowWorkspace;

/*
 * Workflow Revision Entity stores the detail for each version of the workflow in conjunction with Workflow Entity
 * 
 * A number of these elements are relied on by the Workflow model
 */
@Data
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
}
