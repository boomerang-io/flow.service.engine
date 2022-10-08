# Flow Engine Service

_based on Flow's Workflow Service and TektonCD_

## Development

This project uses Gradle to compile

### Local MongoDB w Docker

```
docker run --name local-mongo -d mongo:latest
```

## Locks

For distributed locking, we use this [distributed lock](https://github.com/alturkovic/distributed-lock) project with the Mongo implementation.

## Change Log

The following attempts to list the changes from Workflow Service to Workflow Engine

| Original | Change | Description |
| --- | --- | --- |
| Workflow / Task Activity | Workflow / Task Run | Activity may resonate more with Users however Run is a more known term in the cloud-native industry. It can be still called Activity on the front end. |
| `workflows_` collection prefix | `workflow_` | The prefix plural is now on the end i.e. `workflow_runs` |
| `workflows_activities_tasks` | `task_runs` | Better represents what the collection is. |

## Error Handling

The following provides design and reference information about the status codes and error messages.

### Response Format

The format can be seen in `io.boomerang.error.ErrorDetail.java`

| Field | Description|
| --- | --- |
| code | unique identifier (int) that can be read and understood that detect and handle errors programmatically. |
| reason | unique identifier (string) that can be used to quickly identify and search for more information regarding the error. |
| message | a description of the error intended for a human and an end user to provide context. |
| status | HTTP Status Code Message |


```java
{
  "code": 1001
  "reason": "QUERY_INVALID_FILTERS",
  "message": "Invalid query filters(status) have been provided.",
  "status": "BAD_REQUEST"
}
```

### Implementation

The implementation allows for known and custom exceptions in the code.

Known codes are indexed in the `io.boomerang.error.BoomerangError.java` with the message text in `messages.properties`. Alternatively, a custom exception can be thrown in the code however this will lose the benefit of localization (_future_) 
