package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TaskTemplateRevisionEntity;

public interface TaskTemplateRevisionRepository extends MongoRepository<TaskTemplateRevisionEntity, String> {
  Integer countByParentRef(String parent);

  List<TaskTemplateRevisionEntity> findByParentRef(String parent);
  
  Optional<TaskTemplateRevisionEntity> findByParentRefAndVersion(String parent, Integer version);
  
  @Aggregation(pipeline = {
          "{'$match':{'parentRef': ?0}}",
          "{'$sort': {version: -1}}",
          "{'$limit': 1}"
    })
  Optional<TaskTemplateRevisionEntity> findByParentRefAndLatestVersion(String parent);

  void deleteByParentRef(String parent);
}
