package io.boomerang.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.entity.WorkflowEntity;
import io.boomerang.repository.WorkflowRepository;

/*
 * Service implements the CRUD ops on a Workflow
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

  @Autowired
  private WorkflowRepository workflowRepository;
  
  @Override
  public WorkflowEntity getWorkflow(String workflowId) {

    final Optional<WorkflowEntity> entity = workflowRepository.findById(workflowId);

    //TODO: filter sensitive inputs/results
    //TODO: handle not found with Exception.
    return entity.orElse(null);
  }
}
