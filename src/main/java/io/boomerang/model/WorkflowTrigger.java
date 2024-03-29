package io.boomerang.model;

/*
 * This is a fixed trigger model due to the UI. 
 * 
 * TODO: in future you could have a List<Trigger> in Workflow and delete this class
 */
public class WorkflowTrigger {

  private Trigger manual = new Trigger(true);
  private Trigger schedule = new Trigger(false);
  private Trigger webhook = new Trigger(false);
  private Trigger event = new Trigger(false);
  private Trigger github = new Trigger(false);

  @Override
  public String toString() {
    return "WorkflowTrigger [manual=" + manual + ", schedule=" + schedule + ", webhook=" + webhook
        + ", event=" + event + ", github=" + github + "]";
  }

  public Trigger getManual() {
    return manual;
  }

  public void setManual(Trigger manual) {
    this.manual = manual;
  }

  public Trigger getSchedule() {
    return schedule;
  }

  public void setSchedule(Trigger schedule) {
    this.schedule = schedule;
  }

  public Trigger getWebhook() {
    return webhook;
  }

  public void setWebhook(Trigger webhook) {
    this.webhook = webhook;
  }

  public Trigger getEvent() {
    return event;
  }

  public void setEvent(Trigger event) {
    this.event = event;
  }

  public Trigger getGithub() {
    return github;
  }

  public void setGithub(Trigger github) {
    this.github = github;
  }
}
