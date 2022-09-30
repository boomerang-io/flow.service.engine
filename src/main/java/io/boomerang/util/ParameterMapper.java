package io.boomerang.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.boomerang.model.KeyValuePair;
import io.boomerang.model.tekton.Annotations;
import io.boomerang.model.tekton.Labels;

public class ParameterMapper {
  
  /*
   * Converts a Parameter Map to a KeyValuePair List.
   * This makes it safe for MongoDB to store the parameters if there are dots in the key.
   * 
   * Will check if parameterMap is null and return a new LinkedList.
   * 
   * @param the parameter map, typically from a model
   * @return the parameter keyvalue list, typically to save into an entity.
   */
  public static List<KeyValuePair> mapToKeyValuePairList(Map<String, String> parameterMap) {
    List<KeyValuePair> parameterList = new LinkedList<>();
    if (parameterMap != null) {
      for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
        String key = entry.getKey();
        String value = parameterMap.get(key);
        KeyValuePair prop = new KeyValuePair();
        prop.setKey(key);
        prop.setValue(value);
        parameterList.add(prop);
      }
    }
    return parameterList;
  }
  
  public static Map<String, String> keyValuePairListToMap(List<KeyValuePair> parameterList) {
    Map<String, String> parameterMap = new HashMap<>();
    if (parameterList != null) {
      parameterList.stream().forEach(p -> {
        parameterMap.put(p.getKey(), p.getValue());
      });
    }
    return parameterMap;
  }
  
  //TODO: Do we need this otherFields / unknownFields with a think vs just a Map?
  public static List<KeyValuePair> labelsToKeyValuePairList(Labels labels) {
    List<KeyValuePair> parameterList = new LinkedList<>();
    if (labels != null && labels.otherFields() != null) {
      Map<String, String> labelMap = labels.otherFields();
      for (Map.Entry<String, String> entry : labelMap.entrySet()) {
        String key = entry.getKey();
        String value = labelMap.get(key);
        KeyValuePair prop = new KeyValuePair();
        prop.setKey(key);
        prop.setValue(value);
        parameterList.add(prop);
      }
    }
    return parameterList;
  }
  
  public static Labels keyValuePairListToLabels(List<KeyValuePair> labelPairs) {
    Labels labels = new Labels();
    if (labelPairs != null) {
      for (KeyValuePair l : labelPairs) {
        labels.setOtherField(l.getKey(), l.getValue());
      }
    }
    return labels;
  }
  
  public static List<KeyValuePair> annotationsToKeyValuePairList(Annotations annotations) {
    List<KeyValuePair> parameterList = new LinkedList<>();
    if (annotations != null && annotations.otherFields() != null) {
      Map<String, Object> annotationsMap = annotations.otherFields();
      for (Map.Entry<String, Object> entry : annotationsMap.entrySet()) {
        String key = entry.getKey();
        Object value = annotationsMap.get(key);
        KeyValuePair prop = new KeyValuePair();
        prop.setKey(key);
        prop.setValue(value.toString());
        parameterList.add(prop);
      }
    }
    return parameterList;
  }
  
  public static Annotations keyValuePairListToAnnotations(List<KeyValuePair> annotationPairs) {
    Annotations annotations = new Annotations();
    if (annotationPairs != null) {
      for (KeyValuePair a : annotationPairs) {
        annotations.setOtherField(a.getKey(), (Object) a.getValue());
      }
    }
    return annotations;
  }
}
