package io.boomerang.data.model;

import java.util.List;
import io.boomerang.model.ParamSpec;

public class TaskTemplateSpec {

  private List<String> arguments;
  private List<String> command;
  private List<ParamSpec> params;
  private List<TaskEnvVar> envs;
  private String image;
  private List<TaskTemplateResult> results;
  private String script;
  private Integer version;
  private String workingDir;
  
  public List<String> getArguments() {
    return arguments;
  }

  public List<String> getCommand() {
    return command;
  }

  public List<ParamSpec> getParams() {
    return params;
  }

  public List<TaskEnvVar> getEnvs() {
    return envs;
  }

  public String getImage() {
    return image;
  }

  public List<TaskTemplateResult> getResults() {
    return results;
  }

  public String getScript() {
    return script;
  }

  public Integer getVersion() {
    return version;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public void setCommand(List<String>  command) {
    this.command = command;
  }

  public void setParams(List<ParamSpec> params) {
    this.params = params;
  }

  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setResults(List<TaskTemplateResult> results) {
    this.results = results;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }
}