package io.boomerang.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfiguration {

  @Value("${mongo.collection.prefix}")
  private String mongoCollectionPrefix;

  public String fullCollectionName(String collectionName) {

    if (mongoCollectionPrefix == null || mongoCollectionPrefix.isBlank()) {
      return "" + collectionName;
    }
    String newCollectionName = mongoCollectionPrefix + "_" + collectionName;

    return newCollectionName;
  }

  public String collectionPrefix() {
    return this.mongoCollectionPrefix;
  }
  
  @Autowired
  public void setMapKeyDotReplacement(MappingMongoConverter mongoConverter) {
    mongoConverter.setMapKeyDotReplacement("#");
}
}
