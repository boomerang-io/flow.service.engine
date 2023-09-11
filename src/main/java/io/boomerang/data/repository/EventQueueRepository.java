package io.boomerang.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.EventQueueEntity;

public interface EventQueueRepository extends MongoRepository<EventQueueEntity, String> {
  
}

