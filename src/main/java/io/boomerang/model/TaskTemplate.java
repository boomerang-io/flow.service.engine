package io.boomerang.model;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.TaskTemplateEntity;
import io.boomerang.data.model.TaskEnvVar;
import io.boomerang.data.model.TaskTemplateResult;
import io.boomerang.data.model.TaskTemplateRevision;

public class TaskTemplate extends TaskTemplateEntity {

  private List<String> arguments;
  private ChangeLog changelog;
  private List<String> command;
  private List<TaskTemplateConfig> params;
  private List<TaskEnvVar> envs;
  private String image;
  private List<TaskTemplateResult> results;
  private String script;
  private Integer version;
  private String workingDir;

  public TaskTemplate() {

  }

  public TaskTemplate(TaskTemplateEntity entity, Optional<Integer> optVersion) {
    BeanUtils.copyProperties(entity, this, "currentVersion", "revisions");
    if (!entity.getRevisions().isEmpty()) {
      Integer version = entity.getRevisions().size();
      if (optVersion.isPresent() && optVersion.get() <= entity.getRevisions().size()) {
        version = optVersion.get();
      }
      TaskTemplateRevision revision = entity.getRevisions().get(version - 1);
      this.arguments = revision.getArguments();
      this.changelog = revision.getChangelog();
      this.command = revision.getCommand();
      this.params = revision.getParams();
      this.envs = revision.getEnvs();
      this.image = revision.getImage();
      this.results = revision.getResults();
      this.script = revision.getScript();
      this.version = revision.getVersion();
      this.workingDir = revision.getWorkingDir();
    }
  }

  public List<String> getArguments() {
    return arguments;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public List<String> getCommand() {
    return command;
  }

  public void setCommand(List<String> command) {
    this.command = command;
  }

  public List<TaskTemplateConfig> getParams() {
    return params;
  }

  public void setParams(List<TaskTemplateConfig> params) {
    this.params = params;
  }

  public List<TaskEnvVar> getEnvs() {
    return envs;
  }

  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public List<TaskTemplateResult> getResults() {
    return results;
  }

  public void setResults(List<TaskTemplateResult> results) {
    this.results = results;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }
}
