package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import io.boomerang.data.entity.WorkflowRevisionEntity;
import io.boomerang.data.model.WorkflowRevisionCount;

public interface WorkflowRevisionRepository
    extends MongoRepository<WorkflowRevisionEntity, String> {

  long countByWorkflowId(String workflowId);

  WorkflowRevisionEntity findByWorkflowIdAndVersion(String workflowId, long version);

  Page<WorkflowRevisionEntity> findByWorkflowId(String string, Pageable pageable);
    
  @Aggregation(pipeline = {
          "{'$match':{'workflowId': ?0}}",
          "{'$sort': {version: -1}}",
          "{'$limit': 1}"
    })
  Optional<WorkflowRevisionEntity> findWorkflowByIdAndLatestVersion(String workflowId);
  
  @Aggregation(pipeline = {
		  "{'$match':{'workflowId': {$in: ?0}}}",
	      "{'$sort':{'workflowId': -1, version: -1}}",
	      "{'$group': { _id: '$workflowId', 'count': { $sum: 1 }}}"
	})
  List<WorkflowRevisionCount> findWorkflowVersionCounts(List<String> workflowIds);

  @Aggregation(pipeline = {
		  "{'$match':{'workflowId': {$in: ?0}}}",
	      "{'$sort':{'workflowId': -1, version: -1}}",
	      "{'$group': { _id: '$workflowId', 'count': { $sum: 1 }, 'latestVersion': {$first: '$$ROOT'}}}"
	})
  List<WorkflowRevisionCount> findWorkflowVersionCountsAndLatestVersion(List<String> workflowIds);

}
