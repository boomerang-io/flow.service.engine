package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import com.github.alturkovic.lock.mongo.impl.SimpleMongoLock;
import com.github.alturkovic.lock.retry.RetriableLock;
import io.boomerang.config.MongoConfiguration;
import io.boomerang.data.entity.TaskRunEntity;
import io.boomerang.model.RunParam;
import io.boomerang.util.ParameterUtil;

@Service
public class LockManagerImpl implements LockManager {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoConfiguration mongoConfiguration;
  
  @Autowired
  private Lock lock;

//  @Autowired
//  private PropertyManager propertyManager;

  private static final Logger LOGGER = LogManager.getLogger(LockManagerImpl.class);
  
  @Override
  public String acquireWorkflowLock(List<String> keys) {
    String storeId = mongoConfiguration.fullCollectionName("task_locks");
    long timeout = 105000;
    
    RetryTemplate retryTemplate = getWorkflowRetryTemplate();
    return retryTemplate.execute(ctx -> {
      final String token = lock.acquire(keys, storeId, timeout);
      if (!StringUtils.isEmpty(token)) {
        throw new LockNotAvailableException(
            String.format("Lock not available for keys: %s in store %s", keys, storeId));
      }
      return token;
    });
  }
  
  @Override
  public void releaseWorkflowLock(List<String> keys, String tokenId) {
    String storeId = mongoConfiguration.fullCollectionName("task_locks");
    lock.release(keys, storeId, tokenId);
  }

  //TODO: determine if we can combine with other getRetryTemplate 
  //or convert into retriable lock
  private RetryTemplate getWorkflowRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(2000l);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(100);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

  @Override
  public void acquireLock(TaskRunEntity taskExecution, String wfRunId) {
    long timeout = 60000;
    String key = null;

    if (taskExecution != null) {
//      String workflowId = taskExecution.getWorkflowRef();

      List<RunParam> params = taskExecution.getParams();
      if (ParameterUtil.containsName(params, "timeout")) {
        String timeoutStr = ParameterUtil.getValue(params, "timeout").toString();
        if (!timeoutStr.isBlank() && NumberUtils.isCreatable(timeoutStr)) {
          timeout = Long.valueOf(timeoutStr);
        }
      }
      
      if (ParameterUtil.containsName(params, "key")) {
        key = ParameterUtil.getValue(params, "key").toString();
        //TODO: implement parameter layering
//        ControllerRequestProperties propertiesList =
//            propertyManager.buildRequestPropertyLayering(null, activityId, workflowId);
//        key = propertyManager.replaceValueWithProperty(key, activityId, propertiesList);
      }
      
      /*
       * Utilize the Distributed Lock library to acquire (and retry endlessly)
       * 
       * - The RetriableLock requires a TTL index on the expiresAt field in the MongoCollection.
       *   This is created by the Loader, if you do not use the loader, you will need to create manually.
       */
      if (key != null) {
        //SimpleMongoLock requires a supplier, we don't want the value to change at a future assign time.
        final String finalKey = key;
        Supplier<String> supplier = () -> finalKey;
        SimpleMongoLock mongoLock = new SimpleMongoLock(supplier, this.mongoTemplate);
        RetryTemplate retryTemplate = getRetryTemplate();
        RetriableLock retryLock = new RetriableLock(mongoLock, retryTemplate);
        String storeId = mongoConfiguration.fullCollectionName("task_locks");
        final List<String> keys = new LinkedList<>();
        keys.add(key);

//        final String token = mongoLock.acquire(keys, storeId, timeout);
        final String token = retryLock.acquire(keys, storeId, timeout);

        if (StringUtils.isEmpty(token)) {
          /** TODO: What to do here. */
          throw new LockNotAvailableException(
              String.format("Lock not available for keys: %s in store %s", keys, storeId));
        }

//        RetryTemplate retryTemplate = getRetryTemplate();
//        retryTemplate.execute(ctx -> {
//          final boolean lockExists = mongoLock.exists(storeId, token);
//          if (lockExists) {
//            throw new LockNotAvailableException(
//                String.format("Lock hasn't been released yet for: %s in store %s", keys, storeId));
//          }
//          return lockExists;
//        });

      } else {
        LOGGER.info("No Acquire Lock Key Found!");
      }
    }
  }

  private RetryTemplate getRetryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(10000l);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(Integer.MAX_VALUE);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

  @Override
  public void releaseLock(TaskRunEntity taskExecution, String wfRunId) {
    String key = null;
    if (taskExecution != null) {
      String workflowId = taskExecution.getWorkflowRef();

      List<RunParam> params = taskExecution.getParams();
      if (ParameterUtil.containsName(params, "key")) {
        key = ParameterUtil.getValue(params, "key").toString();
//        ControllerRequestProperties propertiesList =
//            propertyManager.buildRequestPropertyLayering(taskExecution, wfRunId, workflowId);
//        key = propertyManager.replaceValueWithProperty(key, wfRunId, propertiesList);
      }

      if (key != null) {
        final String finalKey = key;
        Supplier<String> supplier = () -> finalKey;
//        FlowMongoLock mongoLock = new FlowMongoLock(supplier, this.mongoTemplate);
        SimpleMongoLock mongoLock = new SimpleMongoLock(supplier, this.mongoTemplate);
        String storeId = mongoConfiguration.fullCollectionName("task_locks");
        final List<String> keys = new LinkedList<>();
        keys.add(key);
        mongoLock.release(keys, storeId, key);
      }
    }
  }
}
