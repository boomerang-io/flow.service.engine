package io.boomerang.data.entity;

import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*
 * Entity for Manual Action and Approval Action
 * 
 * Shared with the Workflow Service
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('event_queue')}")
public class EventQueueEntity {

  @Id
  private String id;
  private String url;
  private HttpEntity<byte[]> request;
  private Date creationDate = new Date();
  
  public EventQueueEntity() {
  }
  
  public EventQueueEntity(String url, HttpEntity<byte[]> request) {
    this.url = url;
    this.request = request;
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }
  public HttpEntity<byte[]> getRequest() {
    return request;
  }
  public void setRequest(HttpEntity<byte[]> request) {
    this.request = request;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
}
