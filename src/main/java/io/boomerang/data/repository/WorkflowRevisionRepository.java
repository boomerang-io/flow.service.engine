package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.model.WorkflowRevisionCount;

public interface WorkflowRevisionRepository
    extends MongoRepository<WorkflowRevisionEntity, String> {

  Integer countByWorkflowRef(String workflowRef);

  WorkflowRevisionEntity findByWorkflowRefAndVersion(String workflowRef, Integer version);

  Page<WorkflowRevisionEntity> findByWorkflowRef(String string, Pageable pageable);
    
  @Aggregation(pipeline = {
          "{'$match':{'workflowRef': ?0}}",
          "{'$sort': {version: -1}}",
          "{'$limit': 1}"
    })
  Optional<WorkflowRevisionEntity> findByWorkflowRefAndLatestVersion(String workflowRef);
  
  @Aggregation(pipeline = {
		  "{'$match':{'workflowRef': {$in: ?0}}}",
	      "{'$sort':{'workflowRef': -1, version: -1}}",
	      "{'$group': { _id: '$workflowId', 'count': { $sum: 1 }}}"
	})
  List<WorkflowRevisionCount> findWorkflowVersionCounts(List<String> workflowRefs);

  @Aggregation(pipeline = {
		  "{'$match':{'workflowRef': {$in: ?0}}}",
	      "{'$sort':{'workflowRef': -1, version: -1}}",
	      "{'$group': { _id: '$workflowRef', 'count': { $sum: 1 }, 'latestVersion': {$first: '$$ROOT'}}}"
	})
  List<WorkflowRevisionCount> findWorkflowVersionCountsAndLatestVersion(List<String> workflowRefs);

}
