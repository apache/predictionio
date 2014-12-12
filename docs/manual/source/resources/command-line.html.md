---
title: Command Line
---

##Overview

Interaction with PredictionIO is done through the command line interface. It follows the format of: 

```pio <command> [options] <args>...```


Runing ```pio help``` displays the usage summary: 

```
$ pio help
Usage: pio <command> [options] <args>...

Options common to all commands:
  [--pio-home <value>] [--spark-home <value>] [--sbt <value>]
  [-ei <value>] [-ev <value>] [-v <value>] [-m <value>]
  [--verbose] [--debug]
  [<args>] [-- [<args passed to Spark>] [-- [<args passed to runner]]]

  --sbt <value>
      Full path of sbt. Default: sbt
  -ei <value> | --engine-id <value>
      Specify an engine ID. Usually used by distributed deployment.
  -ev <value> | --engine-version <value>
      Specify an engine version. Usually used by distributed deployment.
  -v <value> | --variant <value>
      Path to an engine variant JSON file. Default: engine.json
  -m <value> | --manifest <value>
      Path to an engine manifest JSON file. Default: manifest.json
  --verbose
      Enable third-party informational messages.
  --debug
      Enable all debug messages.

Note that it is possible to supply pass-through arguments at the en
of the command by using a '--' separator, e.g.

  pio train -v my-variant -- --master spark://mycluster:7077

In the example above, the '--master' argument will be passed to the underlying
spark-submit command. Please refer to the usage section for each command for
more information.

The most commonly used pio commands are:
    status        Displays status information about PredictionIO
    version       Displays the version of this command line console
    new           Creates a new engine project
    build         Build an engine at the current directory
    train         Kick off a training using an engine
    deploy        Deploy an engine as an engine server
    eventserver   Launch an Event Server
    app           Manage apps that are used by the Event Server
    accesskey     Manage app access keys

The following are experimental development commands:
    run           Launch a driver program
    eval          Kick off an evaluation using an engine
    dashboard     Launch an evaluation dashboard

See 'pio help <command>' to read about a specific subcommand.
```


##General Commands
```help```          Display usage summary. `pio help <command>` to read about a specific subcommand.
  
```version```       Displays the version of the installed PredictionIO.

```status```        Displays install path and running status of PredictionIO system and its dependencies.


##Event Server Commands
```app```           Manage apps that are used by the Event Server. 

```pio app data-delete <name>``` deletes all data associated with the app. 
  
```pio app delete <name>``` deletes the app and its data.

```eventserver```   Launch an Event Server. 

  ```--ip <value>``` IP to bind to. Default to localhost. 
  
  ```--port <value>``` Port to bind to. Default to 7070.


```accesskey```     Manage app access keys.


##Engine Commands
Engine commands need to be run from the directory that contains the engine project. ```--debug``` and ```--verbose``` flags will provide debug and third-party informational messages.

```build```         Build the engine at the current directory. 

```train```         Kick off a training using an engine.

```deploy```        Deploy an engine as an engine server. If no instance ID is specified, it will deploy the latest instance. 



    

    
    

    



