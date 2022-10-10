package io.boomerang.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.ParamLayers;
import io.boomerang.model.RunParam;
import io.boomerang.util.ParameterUtil;

@Service
public class ParameterManagerImpl implements ParameterManager {
  private static final Logger LOGGER = LogManager.getLogger();

//  @Autowired
//  private FlowSettingsService flowSettingsService;
//
//  @Autowired
//  private RevisionService revisionService;
//
//  @Autowired
//  private WorkflowService workflowService;
//
//  @Autowired
//  private FlowTeamService flowTeamService;
//
//  @Autowired
//  private FlowActivityService activityService;

  @Autowired
  public WorkflowRunRepository workflowRunRepository;
//
//  @Autowired
//  private FlowGlobalConfigService flowGlobalConfigService;
//
//  @Autowired
//  private FlowTaskTemplateService flowTaskTemplateService;

//  @Value("${flow.services.listener.webhook.url}")
//  private String webhookUrl;
//
//
//  @Value("${flow.services.listener.wfe.url}")
//  private String waitForEventUrl;
//
//
//  @Value("${flow.services.listener.event.url}")
//  private String eventUrl;


  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};
  
  @Override
  public void resolveWorkflowRunParams(String wfRunId, List<RunParam> wfRunParams) {
    ParamLayers paramLayers = buildParameterLayering(Optional.of(wfRunParams), Optional.empty());
    resolveParams(wfRunId, wfRunParams, paramLayers);
  }
  
  @Override
  public void resolveTaskRunParams(String wfRunId, List<RunParam> wfRunParams, List<RunParam> taskRunParams) {
    ParamLayers paramLayers = buildParameterLayering(Optional.of(wfRunParams), Optional.of(taskRunParams));
    resolveParams(wfRunId, taskRunParams, paramLayers);
  }
  
  /*
   * Resolve all parameters for a particular set of RunParams
   */
  private void resolveParams(String wfRunId, List<RunParam> runParams, ParamLayers paramLayers) {
    //This model should include an orderedList of the scope layers and then parameters for each layer (similar to a Page object)
    String regex = "(?<=\\$\\().+?(?=\\))";
    Pattern pattern = Pattern.compile(regex);
    runParams.stream().forEach(p -> {
      LOGGER.debug("Resolving Parameters: " + p.getName() + " = " + p.getValue());
      if (p.getValue() instanceof String) {
        p.setValue(replacePropertiesAlternate(p.getValue().toString(), wfRunId, paramLayers));
      } else if (p.getValue() instanceof List) {
        ArrayList<String> valueList = (ArrayList<String>) p.getValue();
        valueList.forEach(v -> {
          v = replacePropertiesAlternate(v, wfRunId, paramLayers);
        });
      } else if (p.getValue() instanceof Object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
          String objectString = mapper.writeValueAsString(p.getValue());
          String replacedObjectString = replacePropertiesAlternate(objectString, wfRunId, paramLayers);
          p.setValue(mapper.readValue(replacedObjectString, Object.class));
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      }
    });
  }

  /*
   * Build all parameter layers as an object of Maps
   * 
   * If you only pass it the Workflow Run Entity, it won't add the Task Run Params to the map
   */
  private ParamLayers buildParameterLayering(Optional<List<RunParam>> wfRunParams, Optional<List<RunParam>> taskRunParams) {
    ParamLayers parameterLayers = new ParamLayers();
    //TODO: retrieve extended parameter layers from Workflow service if the application property / URL has been provided
//    Map<String, String> systemProperties = parameterLayers.getSystemProperties();
//    Map<String, String> globalProperties = parameterLayers.getGlobalProperties();
//    Map<String, String> teamProperties = parameterLayers.getTeamProperties();
//    Map<String, String> workflowProperties = parameterLayers.getWorkflowProperties();
//    Map<String, String> reservedProperties = parameterLayers.getReservedProperties();

//    buildGlobalProperties(globalProperties);
//    buildSystemProperties(task, activityId, workflowId, systemProperties);
//    buildReservedPropertyList(reservedProperties, workflowId);

//    if (flowSettingsService.getConfiguration("features", "teamParameters").getBooleanValue()) {
//      buildTeamProperties(teamProperties, workflowId);
//    }
      if (wfRunParams.isPresent()) {
        parameterLayers.setWorkflowProperties(ParameterUtil.runParamListToMap(wfRunParams.get()));
      }
      if (taskRunParams.isPresent()) {
        parameterLayers.setTaskInputProperties(ParameterUtil.runParamListToMap(taskRunParams.get()));
      }

    return parameterLayers;
  }

  /*
   * Adds Workflow API Tokens as Parameters
   */
//  private void buildReservedPropertyList(Map<String, String> reservedProperties,
//      String workflowId) {
//
//    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//    if (workflow.getTokens() != null) {
//      for (WorkflowToken token : workflow.getTokens()) {
//        reservedProperties.put("system.tokens." + token.getLabel(), token.getToken());
//      }
//    }
//  }

//  private void buildTaskInputProperties(ParamLayers applicationProperties,
//      Task task, String activityId) {
//    ActivityEntity activity = activityService.findWorkflowActivity(activityId);
//
//    List<TaskTemplateConfig> configs = getInputsForTask(task, activity.getWorkflowRevisionid());
//
//    Map<String, String> workflowInputProperties = applicationProperties.getTaskInputProperties();
//    for (TaskTemplateConfig config : configs) {
//      String key = config.getKey();
//      String value = this.getInputForTaskKey(task, activity.getWorkflowRevisionid(), key);
//
//      if (value == null || value.isBlank()) {
//        value = config.getDefaultValue();
//      }
//
//      if (value != null) {
//        String newValue = this.replaceValueWithProperty(value, activityId, applicationProperties);
//        newValue = this.replaceValueWithProperty(newValue, activityId, applicationProperties);
//
//        newValue = this.replaceAllParams(newValue, activityId, applicationProperties);
//
//        workflowInputProperties.put(key, newValue);
//      } else {
//        workflowInputProperties.put(key, "");
//      }
//    }
//  }
//
//  private String getInputForTaskKey(Task task, String revisionId, String key) {
//    Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
//    if (revisionOptional.isPresent()) {
//      RevisionEntity revision = revisionOptional.get();
//      Dag dag = revision.getDag();
//      List<DAGTask> tasks = dag.getTasks();
//      if (tasks != null) {
//        DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
//            .findFirst().orElse(null);
//        if (dagTask != null) {
//          List<KeyValuePair> properties = dagTask.getProperties();
//          if (properties != null) {
//            KeyValuePair property =
//                properties.stream().filter(e -> key.equals(e.getKey())).findFirst().orElse(null);
//            if (property != null) {
//              return property.getValue();
//            }
//          }
//        }
//      }
//    }
//    return null;
//  }
//
//
//  private List<TaskTemplateConfig> getInputsForTask(Task task, String revisionId) {
//    Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
//    if (revisionOptional.isPresent()) {
//      RevisionEntity revision = revisionOptional.get();
//      Dag dag = revision.getDag();
//      List<DAGTask> tasks = dag.getTasks();
//      if (tasks != null) {
//        DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
//            .findFirst().orElse(null);
//        if (dagTask != null) {
//          String templateId = dagTask.getTemplateId();
//          Integer templateVersion = dagTask.getTemplateVersion();
//          FlowTaskTemplateEntity taskTemplate =
//              flowTaskTemplateService.getTaskTemplateWithId(templateId);
//
//          if (taskTemplate != null) {
//            List<Revision> revisions = taskTemplate.getRevisions();
//            if (revisions != null) {
//              Revision rev = revisions.stream().filter(e -> e.getVersion().equals(templateVersion))
//                  .findFirst().orElse(null);
//              if (rev != null && rev.getConfig() != null) {
//                return rev.getConfig();
//              }
//            }
//          }
//        }
//      }
//    }
//    return new LinkedList<>();
//  }
//
//  @Override
//  public void buildGlobalProperties(Map<String, String> globalProperties) {
//    List<FlowGlobalConfigEntity> globalConfigs = this.flowGlobalConfigService.getGlobalConfigs();
//    for (FlowGlobalConfigEntity entity : globalConfigs) {
//      if (entity.getValue() != null) {
//        globalProperties.put(entity.getKey(), entity.getValue());
//      }
//    }
//  }
//
//  @Override
//  public void buildSystemProperties(Task task, String activityId, String workflowId,
//      Map<String, String> systemProperties) {
//
//    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//    if (activityId != null) {
//      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
//      RevisionEntity revision =
//          revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid());
//
//      if (revision != null) {
//        systemProperties.put("workflow-version", Long.toString(revision.getVersion()));
//      }
//      systemProperties.put("trigger-type", activity.getTrigger());
//      systemProperties.put("workflow-activity-initiator", "");
//      if (activity.getInitiatedByUserId() != null) {
//        systemProperties.put("workflow-activity-initiator", activity.getInitiatedByUserId());
//      }
//    }
//
//    systemProperties.put("workflow-name", workflow.getName());
//    systemProperties.put("workflow-activity-id", activityId);
//    systemProperties.put("workflow-id", workflow.getId());
//
//    systemProperties.put("trigger-webhook-url", this.webhookUrl);
//    systemProperties.put("trigger-wfe-url", this.waitForEventUrl);
//    systemProperties.put("trigger-event-url", this.eventUrl);
//
//
//    if (task != null) {
//      systemProperties.put("task-name", task.getTaskName());
//      systemProperties.put("task-id", task.getTaskId());
//      systemProperties.put("task-type", task.getTaskType().toString());
//    }
//  }
//
//  @Override
//  public void buildTeamProperties(Map<String, String> teamProperties, String workflowId) {
//    WorkflowSummary workflow = workflowService.getWorkflow(workflowId);
//
//    if (WorkflowScope.team.equals(workflow.getScope())) {
//      TeamEntity flowTeamEntity = this.flowTeamService.findById(workflow.getFlowTeamId());
//      if (flowTeamEntity == null) {
//        return;
//      }
//
//      List<FlowTeamConfiguration> teamConfig = null;
//      if (flowTeamEntity.getSettings() != null) {
//        teamConfig = flowTeamEntity.getSettings().getProperties();
//      }
//      if (teamConfig != null) {
//        for (FlowTeamConfiguration config : teamConfig) {
//          teamProperties.put(config.getKey(), config.getValue());
//        }
//      }
//    }
//  }

//  @Override
//  public String replaceValueWithProperty(String value, String activityId,
//      ParamLayers properties) {
//
//    String replacementString = value;
//    replacementString = replaceProperties(replacementString, activityId, properties);
//
//    return replacementString;
//  }

  private String replaceProperties(String value, String wfRunId,
      ParamLayers applicationProperties) {

    Map<String, Object> executionProperties = applicationProperties.getFlatMap();
    LOGGER.debug(executionProperties.toString());

    String regex = "(?<=\\$\\().+?(?=\\))";
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(value);
    List<String> originalValues = new LinkedList<>();
    List<String> newValues = new LinkedList<>();
    while (m.find()) {
      String extractedValue = m.group(0);
      String replaceValue = null;

      int start = m.start() - 2;
      int end = m.end() + 1;
      String[] components = extractedValue.split("\\.");

      if (components.length == 2) {
        List<String> reservedList = Arrays.asList(reserved);

        String params = components[0];
        if ("params".equals(params)) {

          String propertyName = components[1];


          if (executionProperties.get(propertyName) != null) {
            replaceValue = executionProperties.get(propertyName).toString();
          } else {
            replaceValue = "";
          }
//        } else if (reservedList.contains(params)) {
//          String key = components[1];
//          if ("allParams".equals(key)) {
//            Map<String, Object> properties = applicationProperties.getMapForKey(params);
//            replaceValue = this.getEncodedPropertiesForMap(properties);
//          }
        }
      } else if (components.length == 4) {

        String task = components[0];
        String taskName = components[1];
        String results = components[2];
        String outputProperty = components[3];

//        if (("task".equals(task) || "tasks".equals(task)) && "results".equals(results)) {
//
//          TaskExecutionEntity taskExecution = getTaskExecutionEntity(activityId, taskName);
//          if (taskExecution != null && taskExecution.getOutputs() != null
//              && taskExecution.getOutputs().get(outputProperty) != null) {
//            replaceValue = taskExecution.getOutputs().get(outputProperty);
//          } else {
//            replaceValue = "";
//          }
//        }
      } else if (components.length == 3) {
        String scope = components[0];
        String params = components[1];
        String name = components[2];
        List<String> reservedList = Arrays.asList(reserved);
//        if ("tokens".equals(params) && "system".equals(scope)) {
//          if (executionProperties.get(extractedValue) != null) {
//            replaceValue = executionProperties.get(extractedValue);
//          } else {
//            replaceValue = "";
//          }
//        } else if ("params".equals(params) && reservedList.contains(scope)) {
//          if (reservedList.contains(scope)) {
//            String key = scope + "/" + name;
//
//            if (executionProperties.get(key) != null) {
//              replaceValue = executionProperties.get(key);
//            } else {
//              replaceValue = "";
//            }
//          }
//        }
      }

      if (replaceValue != null) {
        String regexStr = value.substring(start, end);
        originalValues.add(regexStr);
        newValues.add(replaceValue);
      }
    }

    String[] originalValuesArray = originalValues.toArray(new String[originalValues.size()]);
    String[] newValuesArray = newValues.toArray(new String[newValues.size()]);
    String updatedString = StringUtils.replaceEach(value, originalValuesArray, newValuesArray);
    return updatedString;
  }

  private String replacePropertiesAlternate(String value, String wfRunId,
      ParamLayers applicationProperties) {

    Map<String, Object> executionProperties = applicationProperties.getFlatMap();
    LOGGER.debug(executionProperties.toString());
    final StringSubstitutor substitutor = new StringSubstitutor(executionProperties, "$(", ")");
//    substitutor.setEnableUndefinedVariableException(true);

    String replacedString = value;
    String regex = "(?<=\\$\\().+?(?=\\))";
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(value);
    while (m.find()) {
      LOGGER.debug("Pattern Matched: " + m.toString());
      replacedString = substitutor.replace(value);
    }
    return replacedString;
  }

  private String getEncodedPropertiesForMap(Map<String, String> map) {
    Properties properties = new Properties();

    for (Map.Entry<String, String> entry : map.entrySet()) {
      String originalKey = entry.getKey();
      String value = entry.getValue();
      String modifiedKey = originalKey.replaceAll("-", "\\.");
      properties.put(modifiedKey, value);
    }

    try {
      properties.putAll(map);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      properties.store(outputStream, null);
      String text = outputStream.toString();
      String[] lines = text.split("\\n");

      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        if (!line.startsWith("#")) {
          sb.append(line + '\n');
        }

      }
      String propertiesFile = sb.toString();
      String encodedString = Base64.getEncoder().encodeToString(propertiesFile.getBytes());
      return encodedString;
    } catch (IOException e) {
      return "";
    }
  }

}
