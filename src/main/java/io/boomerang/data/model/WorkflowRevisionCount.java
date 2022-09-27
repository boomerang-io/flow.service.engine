package io.boomerang.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.boomerang.data.entity.WorkflowRevisionEntity;

public class WorkflowRevisionCount {
	
	@JsonProperty("_id")
	private String id;  // workflowId

	private long count; // revision count of the workflow

	private WorkflowRevisionEntity latestVersion; // latest revision of the workflow

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public WorkflowRevisionEntity getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(WorkflowRevisionEntity latestVersion) {
		this.latestVersion = latestVersion;
	}

}
