package io.boomerang.engine.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.engine.model.WorkflowTrigger;
import io.boomerang.engine.model.enums.WorkflowStatus;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows')}")
public class WorkflowEntity {
  
  @Id
  private String id;
  private String name;
  private WorkflowStatus status = WorkflowStatus.active;
  private Date creationDate = new Date();
  private String icon;
  private String description;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private WorkflowTrigger triggers;

}
