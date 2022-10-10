# Welcome to the Contributing Guide

Thank you for wanting to contribute and maintain the worlds best automation engine, or so we believe!

You can read more about our project and community via the [Boomerang Roadmap Repo](https://github.com/boomerang-io/roadmap#want-to-get-involved) and specifically the [how to get involved](https://github.com/boomerang-io/roadmap#want-to-get-involved)

## Adding New Task Types

One of the main extensions to the automation is in new Task Types. These dictact the type of automation that is triggered when we process the type.

Read more about Tasks in our [Getting To Know Tasks](https://www.useboomerang.io/docs/boomerang-flow/getting-to-know/tasks) documentation.

| Type | Description |
| --- | --- |
| template | These are the tasks that are executed by a controller and map mostly to a Tekton Task. They are based on the execution of a container with arguments and varaibles to do a task |
| custom | An extension to Template where by the User provides the details dynamically, they are _not_ configured in the Task Template |
| system | System Tasks are special Tasks that affect the logic of the Workflow and do not execute inside a container, but instead affect the processing of the DAG. |
