package io.boomerang.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.entity.EventEntity;
import io.boomerang.model.Relationship;

public interface EventRepository extends MongoRepository<EventEntity, String> {
  
  List<EventEntity> findByOwner(Relationship owner);

}
