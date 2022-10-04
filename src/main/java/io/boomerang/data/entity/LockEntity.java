package io.boomerang.data.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "#{@mongoConfiguration.fullCollectionName('locks')}")
public class LockEntity {
  @Id
  private String id;
  
  @Indexed(expireAfterSeconds=0)
  private LocalDateTime expireAt;
  
  private String token;
}