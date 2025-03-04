package io.boomerang.engine.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.engine.model.AbstractParam;
import io.boomerang.engine.model.ChangeLog;
import io.boomerang.engine.model.ParamSpec;
import io.boomerang.engine.model.WorkflowTask;
import io.boomerang.engine.model.WorkflowWorkspace;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_templates')}")
public class WorkflowTemplateEntity {
  
  @Id
  @JsonIgnore
  private String id;
  @Indexed
  private String name;
  private String displayName;
  private Date creationDate = new Date();
  @Indexed
  private Integer version;
  private String icon;
  private String description;
  private String markdown;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private List<WorkflowTask> tasks = new LinkedList<>();
  private ChangeLog changelog;
  private List<ParamSpec> params;
  private List<WorkflowWorkspace> workspaces;
  private List<AbstractParam> config;
  private Long timeout;
  private Long retries;
}
