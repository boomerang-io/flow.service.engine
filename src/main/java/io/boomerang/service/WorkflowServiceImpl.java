package io.boomerang.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.WorkflowEntity;
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
    //TODO: Add in the handling of Workspaces
//  if (workflow.getStorage() == null) {
//    workflow.setStorage(new Storage());
//  }
//  if (workflow.getStorage().getActivity() == null) {
//    workflow.getStorage().setActivity(new ActivityStorage());
//  }
    return entity.orElse(null);
  }
}
