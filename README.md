⚠️ this project has been deprecated and the functionality migrated to the [Flow Services monorepo](https://github.com/boomerang-io/flow.services)

# Flow Engine Service

_based on Flow's Workflow Service and TektonCD_

This service focuses on executing the Tasks as part of the Workflow's DAG

## v3 to v4 Change Log

There has been an entire code base refactor from v3 to v4 for the engine. We suggest you read the following information to understand the full breadth of changes

- [Model changes and v3 to v4 model comparison](https://github.com/boomerang-io/roadmap/issues/368)
- [Distributed async architecture change](https://github.com/boomerang-io/architecture/tree/feat-v4)

## Pre-Requisites

This service connects to MongoDB and requires Task Templates and indexes loaded through the Flow Loader. You can run these locally, or alternatively connect to a remote MongoDB service.

### Run Local MongoDB w Docker

```
docker run --name local-mongo -d mongo:latest
```

### Load Boomerang Flow Data

```
docker run -e JAVA_OPTS="-Dspring.data.mongodb.uri=mongodb://localhost:27017/boomerang -Dflow.mongo.collection.prefix=flow -Dspring.profiles.active=flow" --network host --platform linux/amd64 boomerangio/flow-loader:latest
```

## Development

This project uses Gradle to compile

### Build JAR

```
gradle build
```

### Build Docker Locally

```
docker buildx build --platform=linux/amd64 -t flow-engine:latest .
```

> Note: these commands have been validated on an M1 Mac

### Running the Docker Container

```
docker run -e JAVA_OPTS="-Dspring.data.mongodb.uri=mongodb://localhost:27017/boomerang -Dflow.mongo.collection.prefix=flow -Dspring.profiles.active=flow" --platform=linux/amd64 flow-engine:latest 
```

## Dependencies

### Parameters and Results

The implementation is based on Tekton Params and Results.

There is limited support for [Tekton Propagated Object Parameters](https://tekton.dev/docs/pipelines/taskruns/#propagated-object-parameters) in that Tekton requires you to provide the Spec for the JSON Object if you are going to reference child elements. We do not have this constraint, we essentially take the path from what is provided after `params.<param-name>`. 

### Locks

For distributed locking, we use this [distributed lock](https://github.com/alturkovic/distributed-lock) project with the Mongo implementation.

The implementation in `LockManagerImpl.java` relies on the TTL Index for Retries having been added via the `flow.loader`.

## Error Handling

The following provides design and reference information about the status codes and error messages.

### Response Format

The format can be seen in `io.boomerang.error.ErrorDetail.java`

| Field | Description|
| --- | --- |
| timestamp | UTC timestamp of when the error occurred | 
| code | unique identifier (int) that can be read and understood that detect and handle errors programmatically. |
| reason | unique identifier (string) that can be used to quickly identify and search for more information regarding the error. |
| message | a description of the error intended for a human and an end user to provide context. |
| status | HTTP Status Code & Message |
| cause | Optional present if `flow.error.include-cause=true` config property is provided |


```java
{
  "timestamp": "2023-01-31T00:15:12.672+00:00",
  "code": 1001
  "reason": "QUERY_INVALID_FILTERS",
  "message": "Invalid query filters(status) have been provided.",
  "status": "400 BAD_REQUEST",
  "cause": null
}
```

### Implementation

The implementation allows for known and custom exceptions in the code.

Known codes are indexed in the `io.boomerang.error.BoomerangError.java` with the message text in `messages.properties`. Alternatively, a custom exception can be thrown in the code however this will lose the benefit of localization (_future_)

