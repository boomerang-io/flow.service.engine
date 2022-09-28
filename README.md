# Flow Engine Service

_based on Flow's Workflow Service and TektonCD_

## Development

This project uses Gradle to compile

### Local MongoDB w Docker

```
docker run --name local-mongo -d mongo:latest
```

## Change Log

The following attempts to list the changes from Workflow Service to Workflow Engine

| Original | Change | Description |
| --- | --- | --- |
| Workflow / Task Activity | Workflow / Task Run | Activity may resonate more with Users however Run is a more known term in the cloud-native industry. It can be still called Activity on the front end. |
| `workflows_` collection prefix | `workflow_` | The prefix plural is now on the end i.e. `workflow_runs` |
| `workflows_activities_tasks` | `task_runs` | Better represents what the collection is. |