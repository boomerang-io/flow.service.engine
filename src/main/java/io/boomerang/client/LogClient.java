package io.boomerang.client;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface LogClient {

  StreamingResponseBody streamLog(String workflowId, String workflowRunId, String taskRunId);

}
