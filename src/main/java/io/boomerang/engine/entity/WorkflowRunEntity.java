package io.boomerang.engine.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.engine.model.RunParam;
import io.boomerang.engine.model.RunResult;
import io.boomerang.engine.model.WorkflowWorkspace;
import io.boomerang.engine.model.enums.RunPhase;
import io.boomerang.engine.model.enums.RunStatus;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_runs')}")
public class WorkflowRunEntity   {

  @Id
  private String id;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private Date creationDate;
  private Date startTime;
  private long duration = 0;
  private Long timeout;
  private Long retries;
  private Boolean debug;
  private RunStatus status = RunStatus.notstarted;
  private RunPhase phase = RunPhase.pending;
  private RunStatus statusOverride;
  private String statusMessage;
  private boolean isAwaitingApproval;
  private String workflowRef;
  private String workflowRevisionRef;
  private String trigger;
  private String initiatedByRef;
  private List<RunParam> params = new LinkedList<>();
  private List<RunResult> results = new LinkedList<>();
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();
  
  @Override
  public String toString() {
    return "WorkflowRunEntity [id=" + id + ", labels=" + labels + ", annotations=" + annotations
        + ", creationDate=" + creationDate + ", startTime=" + startTime + ", duration=" + duration
        + ", timeout=" + timeout + ", retries=" + retries + ", debug=" + debug + ", status="
        + status + ", phase=" + phase + ", statusOverride=" + statusOverride + ", statusMessage="
        + statusMessage + ", isAwaitingApproval=" + isAwaitingApproval + ", workflowRef="
        + workflowRef + ", workflowRevisionRef=" + workflowRevisionRef + ", trigger=" + trigger
        + ", initiatedByRef=" + initiatedByRef + ", params=" + params + ", results=" + results
        + ", workspaces=" + workspaces + "]";
  }
}
