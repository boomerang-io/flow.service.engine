package io.boomerang.model;

import java.util.LinkedList;
import java.util.List;

public class WorkflowRunInsight {

  private Long totalRuns = 0L;
  private Long concurrentRuns = 0L;
  private Long totalDuration = 0L;
  private Long medianDuration = 0L;
  private List<WorkflowRunSummary> runs = new LinkedList<>();

  public Long getTotalRuns() {
    return totalRuns;
  }

  public void setTotalRuns(Long totalRuns) {
    this.totalRuns = totalRuns;
  }

  public Long getConcurrentRuns() {
    return concurrentRuns;
  }

  public void setConcurrentRuns(Long concurrentRuns) {
    this.concurrentRuns = concurrentRuns;
  }

  public Long getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(Long totalDuration) {
    this.totalDuration = totalDuration;
  }

  public Long getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(Long medianDuration) {
    this.medianDuration = medianDuration;
  }

  public List<WorkflowRunSummary> getRuns() {
    return runs;
  }

  public void setRuns(List<WorkflowRunSummary> runs) {
    this.runs = runs;
  }

}
