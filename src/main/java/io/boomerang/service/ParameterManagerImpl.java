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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.boomerang.client.WorkflowClient;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.data.repository.TaskRunRepository;
import io.boomerang.data.repository.WorkflowRunRepository;
import io.boomerang.model.ParamLayers;
import io.boomerang.model.RunParam;
import io.boomerang.model.RunResult;
import io.boomerang.model.WorkflowRun;
import io.boomerang.util.ParameterUtil;

/*
 * Handles Parameter Substitution and Propagation
 * 
 * Currently only Params of dot notation -> $(params.name)
 * 
 * Future: bracket notation patterns -> params['<param name>'] and params["<param name>"]
 * 
 * Ref: https://github.com/tektoncd/pipeline/blob/e96d82e1030b770096d068b8d0f75295cd7dc4c1/pkg/
 * substitution/substitution.go Ref:
 * https://tekton.dev/docs/pipelines/variables/#fields-that-accept-variable-substitutions
 */
@Service
public class ParameterManagerImpl implements ParameterManager {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String REGEX_DOT_NOTATION = "(?<=\\$\\().+?(?=\\))";

  @Autowired
  public WorkflowRunRepository workflowRunRepository;

  @Autowired
  public TaskRunRepository taskRunRepository;

  @Autowired
  public WorkflowClient workflowClient;

  @Value("${flow.workflow.params.enabled}")
  private boolean workflowParamsEnabled;

  final String[] reservedScope = {"global", "team", "workflow", "context"};

  /*
   * Resolve all parameters for a particular set of RunParams
   */
  @Override
  public void resolveParamLayers(WorkflowRunEntity wfRun, Optional<TaskRunEntity> optTaskRun) {
    ParamLayers paramLayers =
        buildParameterLayering(wfRun, optTaskRun);
    List<RunParam> runParams;
    String wfRunId = wfRun.getId();
    if (optTaskRun.isPresent()) {
      runParams = optTaskRun.get().getParams();
    } else {
      runParams = wfRun.getParams();
    }
    // This model should include an orderedList of the scope layers and then parameters for each
    // layer (similar to a Page object)
    runParams.stream().forEach(p -> {
      LOGGER.debug("Resolving Parameters: " + p.getName() + " = " + p.getValue());
      if (p.getValue() instanceof String) {
        p.setValue(resolveParam(p.getValue().toString(), wfRunId, paramLayers));
      } else if (p.getValue() instanceof List) {
        ArrayList<String> valueList = (ArrayList<String>) p.getValue();
        valueList.forEach(v -> {
          v = (String) resolveParam(v, wfRunId, paramLayers);
        });
      } else if (p.getValue() instanceof Object) {
        // ObjectMapper mapper = new ObjectMapper();
        // try {
        // String objectString = mapper.writeValueAsString(p.getValue());
        // String replacedObjectString =
        // replacePropertiesAlternate(objectString, wfRunId, paramLayers);
        // p.setValue(mapper.readValue(replacedObjectString, Object.class));
        // } catch (JsonProcessingException e) {
        // e.printStackTrace();
        // }
        p.setValue(resolveParam(p.getValue(), wfRunId, paramLayers));
      }
    });
  }

  /*
   * Build all parameter layers as an object of Maps
   * 
   * If you only pass it the Workflow Run Entity, it won't add the Task Run Params to the map
   */
  private ParamLayers buildParameterLayering(WorkflowRunEntity wfRun,
      Optional<TaskRunEntity> optTaskRun) {
    ParamLayers paramLayers = new ParamLayers();
    
    if (workflowParamsEnabled) {
      //Retrieve Global, Team, and some Context from the Workflow Service
      paramLayers = workflowClient.getParamLayers(wfRun.getWorkflowRef());
    }
    //Override particular context Parameters. Additional Context Params come from the Workflow service.
    Map<String, Object> contextParams = paramLayers.getContextParams();
    contextParams.put("workflowrun-trigger", wfRun.getTrigger());
    contextParams.put("workflowrun-initiator", wfRun.getInitiatedByRef().isBlank() ? "" : wfRun.getInitiatedByRef());
    contextParams.put("workflowrun-id", wfRun.getId());
    contextParams.put("workflow-id", wfRun.getWorkflowRef());
    contextParams.put("workflow-name", "");
    contextParams.put("workflow-version", "");
    if (optTaskRun.isPresent()) {
      contextParams.put("taskrun-id", optTaskRun.get().getId());
      contextParams.put("taskrun-name", optTaskRun.get().getName());
      contextParams.put("taskrun-type", optTaskRun.get().getType());
    }
    if (wfRun.getParams() != null && !wfRun.getParams().isEmpty()) {
      paramLayers.setWorkflowParams(ParameterUtil.runParamListToMap(wfRun.getParams()));
    }
    if (optTaskRun.isPresent() && optTaskRun.get().getParams() != null && ! optTaskRun.get().getParams().isEmpty()) {
      paramLayers.setTaskParams(ParameterUtil.runParamListToMap( optTaskRun.get().getParams()));
    }

    return paramLayers;
  }

  // private void buildTaskInputProperties(ParamLayers applicationProperties,
  // Task task, String activityId) {
  // ActivityEntity activity = activityService.findWorkflowActivity(activityId);
  //
  // List<TaskTemplateConfig> configs = getInputsForTask(task, activity.getWorkflowRevisionid());
  //
  // Map<String, String> workflowInputProperties = applicationProperties.getTaskInputProperties();
  // for (TaskTemplateConfig config : configs) {
  // String key = config.getKey();
  // String value = this.getInputForTaskKey(task, activity.getWorkflowRevisionid(), key);
  //
  // if (value == null || value.isBlank()) {
  // value = config.getDefaultValue();
  // }
  //
  // if (value != null) {
  // String newValue = this.replaceValueWithProperty(value, activityId, applicationProperties);
  // newValue = this.replaceValueWithProperty(newValue, activityId, applicationProperties);
  //
  // newValue = this.replaceAllParams(newValue, activityId, applicationProperties);
  //
  // workflowInputProperties.put(key, newValue);
  // } else {
  // workflowInputProperties.put(key, "");
  // }
  // }
  // }
  //
  // private String getInputForTaskKey(Task task, String revisionId, String key) {
  // Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
  // if (revisionOptional.isPresent()) {
  // RevisionEntity revision = revisionOptional.get();
  // Dag dag = revision.getDag();
  // List<DAGTask> tasks = dag.getTasks();
  // if (tasks != null) {
  // DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
  // .findFirst().orElse(null);
  // if (dagTask != null) {
  // List<KeyValuePair> properties = dagTask.getProperties();
  // if (properties != null) {
  // KeyValuePair property =
  // properties.stream().filter(e -> key.equals(e.getKey())).findFirst().orElse(null);
  // if (property != null) {
  // return property.getValue();
  // }
  // }
  // }
  // }
  // }
  // return null;
  // }
  //
  //
  // private List<TaskTemplateConfig> getInputsForTask(Task task, String revisionId) {
  // Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
  // if (revisionOptional.isPresent()) {
  // RevisionEntity revision = revisionOptional.get();
  // Dag dag = revision.getDag();
  // List<DAGTask> tasks = dag.getTasks();
  // if (tasks != null) {
  // DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
  // .findFirst().orElse(null);
  // if (dagTask != null) {
  // String templateId = dagTask.getTemplateId();
  // Integer templateVersion = dagTask.getTemplateVersion();
  // FlowTaskTemplateEntity taskTemplate =
  // flowTaskTemplateService.getTaskTemplateWithId(templateId);
  //
  // if (taskTemplate != null) {
  // List<Revision> revisions = taskTemplate.getRevisions();
  // if (revisions != null) {
  // Revision rev = revisions.stream().filter(e -> e.getVersion().equals(templateVersion))
  // .findFirst().orElse(null);
  // if (rev != null && rev.getConfig() != null) {
  // return rev.getConfig();
  // }
  // }
  // }
  // }
  // }
  // }
  // return new LinkedList<>();
  // }
  //

  // @Override
  // public String replaceValueWithProperty(String value, String activityId,
  // ParamLayers properties) {
  //
  // String replacementString = value;
  // replacementString = replaceProperties(replacementString, activityId, properties);
  //
  // return replacementString;
  // }

  /*
   * Original v3 method
   */
//  private String replaceProperties(String value, String wfRunId,
//      ParamLayers applicationProperties) {
//
//    Map<String, Object> executionProperties = applicationProperties.getFlatMap();
//    LOGGER.debug(executionProperties.toString());
//
//    String regex = "(?<=\\$\\().+?(?=\\))";
//    Pattern pattern = Pattern.compile(regex);
//    Matcher m = pattern.matcher(value);
//    List<String> originalValues = new LinkedList<>();
//    List<String> newValues = new LinkedList<>();
//    while (m.find()) {
//      String extractedValue = m.group(0);
//      String replaceValue = null;
//
//      int start = m.start() - 2;
//      int end = m.end() + 1;
//      String[] components = extractedValue.split("\\.");
//
//      if (components.length == 2) {
//        List<String> reservedList = Arrays.asList(reserved);
//
//        String params = components[0];
//        if ("params".equals(params)) {
//
//          String propertyName = components[1];
//
//
//          if (executionProperties.get(propertyName) != null) {
//            replaceValue = executionProperties.get(propertyName).toString();
//          } else {
//            replaceValue = "";
//          }
//       } else if (reservedList.contains(params)) {
//         String key = components[1];
//         if ("allParams".equals(key)) {
//         Map<String, Object> properties = applicationProperties.getMapForKey(params);
//         replaceValue = this.getEncodedPropertiesForMap(properties);
//       }
//      } else if (components.length == 4) {
//
//        String task = components[0];
//        String taskName = components[1];
//        String results = components[2];
//        String outputProperty = components[3];
//
//        // if (("task".equals(task) || "tasks".equals(task)) && "results".equals(results)) {
//        //
//        // TaskExecutionEntity taskExecution = getTaskExecutionEntity(activityId, taskName);
//        // if (taskExecution != null && taskExecution.getOutputs() != null
//        // && taskExecution.getOutputs().get(outputProperty) != null) {
//        // replaceValue = taskExecution.getOutputs().get(outputProperty);
//        // } else {
//        // replaceValue = "";
//        // }
//        // }
//      } else if (components.length == 3) {
//        String scope = components[0];
//        String params = components[1];
//        String name = components[2];
//        List<String> reservedList = Arrays.asList(reserved);
//        // if ("tokens".equals(params) && "system".equals(scope)) {
//        // if (executionProperties.get(extractedValue) != null) {
//        // replaceValue = executionProperties.get(extractedValue);
//        // } else {
//        // replaceValue = "";
//        // }
//        // } else if ("params".equals(params) && reservedList.contains(scope)) {
//        // if (reservedList.contains(scope)) {
//        // String key = scope + "/" + name;
//        //
//        // if (executionProperties.get(key) != null) {
//        // replaceValue = executionProperties.get(key);
//        // } else {
//        // replaceValue = "";
//        // }
//        // }
//        // }
//      }
//
//      if (replaceValue != null) {
//        String regexStr = value.substring(start, end);
//        originalValues.add(regexStr);
//        newValues.add(replaceValue);
//      }
//    }
//
//    String[] originalValuesArray = originalValues.toArray(new String[originalValues.size()]);
//    String[] newValuesArray = newValues.toArray(new String[newValues.size()]);
//    String updatedString = StringUtils.replaceEach(value, originalValuesArray, newValuesArray);
//    return updatedString;
//  }

//  /*
//   * Initial v4 method that replaced only for String Params
//   */
//  private String replaceStringParameters(String value, String wfRunId, ParamLayers paramLayers) {
//    LOGGER.debug("Value: " + value);
//    String replacedValue = value;
//    Pattern pattern = Pattern.compile(REGEX_DOT_NOTATION);
//    Matcher m = pattern.matcher(value);
//    while (m.find()) {
//      String variableKey = m.group(0);
//      String[] splitVariableKey = variableKey.split("\\.");
//      LOGGER.debug("Key: " + variableKey + ", length: " + splitVariableKey.length);
//
//      // TODO: add in reserved list check
//      if (("params".equals(splitVariableKey[0]) && !(splitVariableKey.length > 2))
//          || ("params".equals(splitVariableKey[1]) && !(splitVariableKey.length > 3))) {
//        // Resolve references to Params
//        final StringSubstitutor substitutor =
//            new StringSubstitutor(paramLayers.getFlatMap(), "$(", ")");
//        // substitutor.setEnableUndefinedVariableException(true);
//        replacedValue = substitutor.replace("$(" + variableKey + ")");
//      } else if ("results".equals(splitVariableKey[2]) && !(splitVariableKey.length > 4)) {
//        // Resolve references to Results
//        String taskName = splitVariableKey[1];
//        String resultName = splitVariableKey[3];
//        Optional<TaskRunEntity> taskRunEntity =
//            taskRunRepository.findFirstByNameAndWorkflowRunRef(taskName, wfRunId);
//        if (taskRunEntity.isPresent()) {
//          List<RunResult> taskRunResults = taskRunEntity.get().getResults();
//          if (!taskRunResults.isEmpty()) {
//            Optional<RunResult> result =
//                taskRunResults.stream().filter(p -> resultName.equals(p.getName())).findFirst();
//
//            if (result.isPresent()) {
//              replacedValue = result.get().getValue().toString();
//            }
//          }
//        }
//      }
//    }
//    LOGGER.debug("Pattern Matched: " + m.toString() + " = " + replacedValue);
//    return replacedValue;
//  }
//
//  /*
//   * Initial v4 method that replaced only for Array Params
//   */
//  private ArrayList<String> replaceArrayParameters(ArrayList<String> values, String wfRunId,
//      ParamLayers paramLayers) {
//    values.forEach(v -> {
//      v = replaceStringParameters(v, wfRunId, paramLayers);
//    });
//    return values;
//  }
//
//  /*
//   * Initial v4 method that replaced only for Object Params
//   */
//  private Object replaceObjectParameters(Object value, String wfRunId, ParamLayers paramLayers) {
//    LOGGER.debug("Object Value: " + value);
//    Object replacedValue = value;
//    Pattern pattern = Pattern.compile(REGEX_DOT_NOTATION);
//
//    // Not sure if we need to go this crazy or if we can just use toString()
//    // ObjectMapper mapper = new ObjectMapper();
//    // try {
//    // String objectString = mapper.writeValueAsString(value);
//    // } catch (JsonProcessingException e) {
//    // e.printStackTrace();
//    // }
//
//    Matcher m = pattern.matcher(value.toString());
//    while (m.find()) {
//      String variableKey = m.group(0);
//      String[] splitVariableKey = variableKey.split("\\.");
//      String objectPath = "";
//      String searchKey = variableKey;
//      LOGGER.debug("Key: " + variableKey + ", length: " + splitVariableKey.length);
//
//      // TODO: add in reserved list check
//      if ("params".equals(splitVariableKey[0]) && (splitVariableKey.length > 2)) {
//        int index = ordinalIndexOf(variableKey, ".", 2);
//        searchKey = variableKey.substring(0, index);
//        objectPath = variableKey.substring(index + 1);
//      } else if ("params".equals(splitVariableKey[1]) && (splitVariableKey.length > 3)) {
//        int index = ordinalIndexOf(variableKey, ".", 3);
//        searchKey = variableKey.substring(0, index);
//        objectPath = variableKey.substring(index + 1);
//      } else if ("results".equals(splitVariableKey[2]) && (splitVariableKey.length >= 4)) {
//        // Resolves references to TaskRun Results
//        String taskName = splitVariableKey[1];
//        String resultName = splitVariableKey[3];
//        Optional<TaskRunEntity> taskRunEntity =
//            taskRunRepository.findFirstByNameAndWorkflowRunRef(taskName, wfRunId);
//        if (taskRunEntity.isPresent()) {
//          List<RunResult> taskRunResults = taskRunEntity.get().getResults();
//          if (!taskRunResults.isEmpty()) {
//            Optional<RunResult> result =
//                taskRunResults.stream().filter(p -> resultName.equals(p.getName())).findFirst();
//
//            if (result.isPresent()) {
//              if (splitVariableKey.length > 4) {
//                int index = ordinalIndexOf(variableKey, ".", 4);
//                searchKey = variableKey.substring(0, index);
//                objectPath = variableKey.substring(index + 1);
//                Object reducedValue = reduceObjectByJsonPath(objectPath, result.get().getValue());
//                return reducedValue != null ? reducedValue : value;
//              } else {
//                return result.get().getValue().toString();
//              }
//            }
//          }
//        }
//
//      }
//      LOGGER.debug("Key: " + searchKey + ", ObjectPath: " + objectPath);
//      final StringSubstitutor substitutor =
//          new StringSubstitutor(paramLayers.getFlatMap(), "$(", ")");
//      // substitutor.setEnableUndefinedVariableException(true);
//      replacedValue = substitutor.replace("$(" + searchKey + ")");
//      if (!objectPath.isEmpty()) {
//        Object reducedValue = reduceObjectByJsonPath(objectPath, replacedValue);
//        replacedValue = reducedValue != null ? reducedValue : replacedValue;
//      }
//      LOGGER.debug("Pattern Matched: " + m.toString() + " = " + replacedValue);
//    }
//    return replacedValue;
//  }

  private Object resolveParamBySubstitutor(Object value, String wfRunId, ParamLayers paramLayers) {
    Map<String, Object> executionProperties = paramLayers.getFlatMap();
    LOGGER.debug("Parameter Layers: " + paramLayers.toString());
    LOGGER.debug("Value: " + value);
    Object replacedValue = value;
    Pattern pattern = Pattern.compile(REGEX_DOT_NOTATION);

    Matcher m = pattern.matcher(value.toString());
    while (m.find()) {
      String foundKey = m.group(0);
      // Trim $(
      int start = m.start() - 2;
      // Trim )
      int end = m.end() + 1;
      String searchPath = "";
      String searchKey = foundKey;

      // TODO: need to support Array syntax too
      String[] components = foundKey.split("\\.");
      LOGGER.debug("Found reference: " + foundKey + ", length: " + components.length);

      // TODO: provide greater support for JSON Object Path Parameters
      // TODO: split this out into a wrapper function that calls this function and is only called
      // for instanceOf Object
      // TODO: add in reserved list check
      if (components.length >= 2 && "params".equals(components[0])) {
        if (components.length > 2) {
          int index = foundKey.indexOf(".", foundKey.indexOf(".") + 1);
          searchKey = foundKey.substring(0, index);
          searchPath = foundKey.substring(index + 1);
        }
      } else if (components.length >= 3 && "params".equals(components[1])) {
        if (components.length > 3) {
          int index = foundKey.indexOf(".",
              foundKey.indexOf(".", foundKey.indexOf(".") + 1) + 1);
          searchKey = foundKey.substring(0, index);
          searchPath = foundKey.substring(index + 1);
        }
      } else if (components.length >= 4 && "results".equals(components[2])) {
        // Resolves references to TaskRun Results
        // Requires access to the specific TaskRun and does not follow the StringSubstitutor pattern
        String taskName = components[1];
        String resultName = components[3];
        Optional<TaskRunEntity> taskRunEntity =
            taskRunRepository.findFirstByNameAndWorkflowRunRef(taskName, wfRunId);
        if (taskRunEntity.isPresent()) {
          List<RunResult> taskRunResults = taskRunEntity.get().getResults();
          if (!taskRunResults.isEmpty()) {
            Optional<RunResult> result =
                taskRunResults.stream().filter(p -> resultName.equals(p.getName())).findFirst();

            if (result.isPresent()) {
              if (components.length > 4) {
                int index = foundKey.indexOf(".", foundKey.indexOf(".",
                    foundKey.indexOf(".", foundKey.indexOf(".") + 1) + 1) + 1);
                searchKey = foundKey.substring(0, index);
                searchPath = foundKey.substring(index + 1);
                Object reducedValue = reduceObjectByJsonPath(searchPath, result.get().getValue());
                return reducedValue != null ? reducedValue : value;
              } else {
                return result.get().getValue().toString();
              }
            }
          }
        }

      }
      LOGGER.debug("Key: " + searchKey + ", ObjectPath: " + searchPath);
      final StringSubstitutor substitutor = new StringSubstitutor(executionProperties, "$(", ")");
      // substitutor.setEnableUndefinedVariableException(true);
      replacedValue = substitutor.replace("$(" + searchKey + ")");
      if (!searchPath.isEmpty()) {
        Object reducedValue = reduceObjectByJsonPath(searchPath, replacedValue);
        replacedValue = reducedValue != null ? reducedValue : replacedValue;
      }
      LOGGER.debug("Pattern Matched: " + m.toString() + " = " + replacedValue);
    }
    return replacedValue;
  }

  /* 
   * v4 method to resolve param by treating all as JSON Path finding methods
   */
  private Object resolveParam(Object value, String wfRunId, ParamLayers paramLayers) {
    Map<String, Object> flatParamLayers = paramLayers.getFlatMap();
    LOGGER.debug(flatParamLayers.toString());
    Pattern pattern = Pattern.compile(REGEX_DOT_NOTATION);
    Matcher m = pattern.matcher(value.toString());
    // List<String> originalValues = new LinkedList<>();
    // List<String> newValues = new LinkedList<>();
    Object replaceValue = value;
    while (m.find()) {
      String foundKey = m.group(0);
      // Trim $(
      int start = m.start() - 2;
      // Trim )
      int end = m.end() + 1;

      LOGGER.debug("Param Substitution: " + foundKey + ", start: " + start + ", end: " + end);
      String[] separatedKey = foundKey.split("\\.");

      if ((separatedKey.length == 2) && "params".equals(separatedKey[0])) { 
        // Handle flattened - params.<name>
        if (flatParamLayers.get(foundKey) != null) {
          replaceValue = flatParamLayers.get(foundKey);
        }
      } else if ((separatedKey.length > 2) && "params".equals(separatedKey[0])) { 
        // Handle flattened with query for individual child of an object param - params.<name>.<query>
        int index = ordinalIndexOf(foundKey, ".", 2);
        String searchKey = foundKey.substring(0, index);
        String searchPath = foundKey.substring(index + 1);
        if (flatParamLayers.get(searchKey) != null) {
          replaceValue = reduceObjectByJsonPath(searchPath, flatParamLayers.get(searchKey));
        }
      } else if ((separatedKey.length == 3) && "params".equals(separatedKey[1]) && List.of(reservedScope).contains(separatedKey[0])) { 
        // Handle specific scoped param - <scope>.params.<name>
        if (flatParamLayers.get(foundKey) != null) {
          replaceValue = flatParamLayers.get(foundKey);
        }
      } else if ((separatedKey.length > 3) && "params".equals(separatedKey[1]) && List.of(reservedScope).contains(separatedKey[0])) { 
        // Hanlde specific scoped param with query for child of object - <scope>.params.<name>.<query>
        int index = ordinalIndexOf(foundKey, ".", 3);
        String searchKey = foundKey.substring(0, index);
        String searchPath = foundKey.substring(index + 1);
        if (flatParamLayers.get(searchKey) != null) {
          replaceValue = reduceObjectByJsonPath(searchPath, flatParamLayers.get(searchKey));
        }
      } else if ((separatedKey.length >= 4) && "tasks".equals(separatedKey[0])
          && "results".equals(separatedKey[2])) {
        // Handle references to TaskRun Results
        String taskName = separatedKey[1];
        String resultName = separatedKey[3];
        Optional<TaskRunEntity> taskRunEntity =
            taskRunRepository.findFirstByNameAndWorkflowRunRef(taskName, wfRunId);
        if (taskRunEntity.isPresent()) {
          List<RunResult> taskRunResults = taskRunEntity.get().getResults();
          if (!taskRunResults.isEmpty()) {
            Optional<RunResult> result =
                taskRunResults.stream().filter(p -> resultName.equals(p.getName())).findFirst();
            if (result.isPresent()) {
              if (separatedKey.length > 4) {
                int index = ordinalIndexOf(foundKey, ".", 4);
                String searchPath = foundKey.substring(index + 1);
                Object reducedValue = reduceObjectByJsonPath(searchPath, result.get().getValue());
                replaceValue = reducedValue != null ? reducedValue : value;
              } else {
                replaceValue = result.get().getValue();
              }
            }
          }
        }
      }
      // TODO: add replace value back into m so that we can do recursive replace
    }
    return replaceValue;
  }

//  private String getEncodedPropertiesForMap(Map<String, String> map) {
//    Properties properties = new Properties();
//
//    for (Map.Entry<String, String> entry : map.entrySet()) {
//      String originalKey = entry.getKey();
//      String value = entry.getValue();
//      String modifiedKey = originalKey.replaceAll("-", "\\.");
//      properties.put(modifiedKey, value);
//    }
//
//    try {
//      properties.putAll(map);
//      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//      properties.store(outputStream, null);
//      String text = outputStream.toString();
//      String[] lines = text.split("\\n");
//
//      StringBuilder sb = new StringBuilder();
//      for (String line : lines) {
//        if (!line.startsWith("#")) {
//          sb.append(line + '\n');
//        }
//
//      }
//      String propertiesFile = sb.toString();
//      String encodedString = Base64.getEncoder().encodeToString(propertiesFile.getBytes());
//      return encodedString;
//    } catch (IOException e) {
//      return "";
//    }
//  }

  private Object reduceObjectByJsonPath(String path, Object object) {
    // Configuration jsonConfig = Configuration.builder().mappingProvider(new
    // JacksonMappingProvider())
    // .jsonProvider(new JacksonJsonNodeJsonProvider()).options(Option.DEFAULT_PATH_LEAF_TO_NULL)
    // .build();

    Configuration jsonConfig =
        Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
    try {
      ObjectMapper mapper = new ObjectMapper();
      // try {
      // String objectString = mapper.writeValueAsString(p.getValue());
      // String replacedObjectString =
      // replacePropertiesAlternate(objectString, wfRunId, paramLayers);
      // p.setValue(mapper.readValue(replacedObjectString, Object.class));

      // String json = object instanceof String ? new JsonObject(object.toString()) : new
      // ObjectMapper().writeValueAsString(object);
      DocumentContext jsonContext = JsonPath.using(jsonConfig).parse(object);
      if (path != null && !path.isBlank() && object != null) {
        Object value = jsonContext.read("$." + path);
        // return value.toString().replaceAll("^\"+|\"+$", "");
        return value;
      }
    } catch (Exception e) {
      // Log and drop exception. We want the workflow to continue execution.
      LOGGER.error(e.toString());
    }
    return null;
  }

  private static int ordinalIndexOf(String str, String substr, int n) {
    int pos = str.indexOf(substr);
    while (--n > 0 && pos != -1)
      pos = str.indexOf(substr, pos + 1);
    return pos;
  }

}
