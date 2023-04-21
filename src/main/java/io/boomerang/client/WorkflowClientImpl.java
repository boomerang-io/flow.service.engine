package io.boomerang.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.ParamLayers;

@Service
@Primary
public class WorkflowClientImpl implements WorkflowClient {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.workflow.paramlayers.url}")
  private String workflowParamsURL;

  @Autowired
  public RestTemplate restTemplate;

  @Override
  public ParamLayers getParamLayers(String workflowId) {
    try {
      String url = workflowParamsURL.replace("{workflowId}", workflowId);
      LOGGER.info("URL: " + url);

      ResponseEntity<ParamLayers> response = restTemplate.getForEntity(url, ParamLayers.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());

      return response.getBody();
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}