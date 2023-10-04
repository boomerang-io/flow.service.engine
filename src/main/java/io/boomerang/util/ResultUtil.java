package io.boomerang.util;

import java.util.List;
import java.util.stream.Collectors;
import io.boomerang.model.ResultSpec;
import io.boomerang.model.RunResult;

public class ResultUtil {
  
  /*
   * Convert ResultSpec to RunResult
   * 
   * @param the parameter list
   * @param the new parameter to add
   * @return the parameter list
   */
  public static List<RunResult> resultSpecToRunResult(List<ResultSpec> resultList) {
    return resultList.stream().map(r -> {
      RunResult result = new RunResult();
      result.setName(r.getName());
      result.setDescription(r.getDescription());
      return result;
    }).collect(Collectors.toList());
  }
}
