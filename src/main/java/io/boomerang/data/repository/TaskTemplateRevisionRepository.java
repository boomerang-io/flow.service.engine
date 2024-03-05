package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskTemplateRevisionEntity;

public interface TaskTemplateRevisionRepository extends MongoRepository<TaskTemplateRevisionEntity, String> {
  Integer countByParent(String parent);

  List<TaskTemplateRevisionEntity> findByParent(String parent);
  
  Optional<TaskTemplateRevisionEntity> findByParentAndVersion(String parent, Integer version);
  
  @Aggregation(pipeline = {
          "{'$match':{'parent': ?0}}",
          "{'$sort': {version: -1}}",
          "{'$limit': 1}"
    })
  Optional<TaskTemplateRevisionEntity> findByParentAndLatestVersion(String parent);

  void deleteByParent(String parent);
}
