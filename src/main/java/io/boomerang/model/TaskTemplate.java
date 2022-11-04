package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.TaskTemplateEntity;

public class TaskTemplate extends TaskTemplateEntity {

  public TaskTemplate() {

  }

  public TaskTemplate(TaskTemplateEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }
}
