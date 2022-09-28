package io.boomerang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
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
}
