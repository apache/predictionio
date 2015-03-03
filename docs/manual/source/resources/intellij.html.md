---
title:  Developing Engines with IntelliJ IDEA
---

##Download

- Download and install [IntelliJ
  IDEA](https://www.jetbrains.com/idea/download/).
- Download and build PredictionIO from the source code.

```
$ git clone https://github.com/PredictionIO/PredictionIO.git
$ cd PredictionIO
$ git checkout master
$ ./make-distribution.sh
```

- git clone [Apache Spark v1.2](https://github.com/apache/spark/tree/v1.2.0) and
  run build/sbt assembly to build.

- Install and start HBase and Elasticsearch. You can refer the install
  instruction [here](http://0.0.0.0:4567/install/install-sourcecode/).

## Setup

### Install Scala Plugin
Run IntelliJ IDEA, install the [Scala
plugin](https://plugins.jetbrains.com/plugin/?id=1347).

Click on Install JetBrains plugin.
![Scala Plugin Install](/images/intellij/intelliJ-scala-plugin.png)

![Scala Plugin Install](/images/intellij/intellij-scala-plugin-2.png)

Restart IntelliJ IDEA if asked to do so.

### Set Up the Engine Directory

From IDEA, open project, browse to the engine directory, choose the `build.sbt`
and open. You may need to set the Project SDK if you have not done it before.

You should be able to make everything compile at this point. To run and debug
PredictionIO server, continue on to the rest of the steps.

Edit build.sbt, add the following under `libraryDependencies`

```
"org.mortbay.jetty" % "servlet-api" % "3.0.20100224"

"io.prediction" %% "core" % "0.8.6-SNAPSHOT" % "provided"
  exclude("javax.servlet", "servlet-api")
```

If you are running on OS X, you will need to add the following as well due to
this [known issue](http://bit.ly/12Abtvn).

```
"org.xerial.snappy" % "snappy-java" % "1.1.1.6"
```

![Build SBT](/images/intellij/intellij-buildsbt.png)

When you are done editing, IntelliJ should either refresh the project
automatically or prompt you to refresh.

### Dependencies
Right click on the project and open module settings (or press F4). Under
Modules, go to the Dependencies tab on the right hand side, add the following
two JARs:

- `PredictionIO/assembly/pio-assembly-0.8.X-SNAPSHOT.jar`

- `spark/assmebly/target/scala-2.10/spark-assembly-1.2.0-hadoop1.0.4.jar`


![Build SBT](/images/intellij/intellij-dependencies.png)



## Run/Debug in IDEA
To run/debug PredictionIO server. First run `pio build` and `pio train` on
command line as usual. Note down the engine instance ID after `pio train`.

WARNING: Please see [Recommendation Engine Template
Quickstart](/templates/recommendation/quickstart) for `pio build` and `pio
train`.


Create a new Run/Debug configuration (Under Run --> Edit Configuration
--> Application).

```
Main class: io.prediction.workflow.CreateServer
VM options: -Dspark.master=local -Dlog4j.configuration=file:/**replace_w_your_predictionIO_path**/conf/log4j.properties
Program Arguments: --engineInstanceId **replace_w_the_id_from_pio_train** --ip localhost --port 8000
```
Environment variables:

```
SPARK_HOME=/**reaplce_w_your_spark_binary_path**
PIO_FS_BASEDIR=/**replace_w_your_path_to**/.pio_store
PIO_FS_ENGINESDIR=/**replace_w_your_path_to**/.pio_store/engines
PIO_FS_TMPDIR=/**replace_w_your_path_to*/.pio_store/tmp
PIO_STORAGE_REPOSITORIES_METADATA_NAME=predictionio_metadata
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=ELASTICSEARCH
PIO_STORAGE_REPOSITORIES_MODELDATA_NAME=pio_
PIO_STORAGE_REPOSITORIES_MODELDATA_SOURCE=LOCALFS
PIO_STORAGE_REPOSITORIES_APPDATA_NAME=predictionio_appdata
PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=ELASTICSEARCH
PIO_STORAGE_REPOSITORIES_EVENTDATA_NAME=predictionio_eventdata
PIO_STORAGE_REPOSITORIES_EVENTDATA_SOURCE=HBASE
PIO_STORAGE_SOURCES_ELASTICSEARCH_TYPE=elasticsearch
PIO_STORAGE_SOURCES_ELASTICSEARCH_HOSTS=localhost
PIO_STORAGE_SOURCES_ELASTICSEARCH_PORTS=9300
PIO_STORAGE_SOURCES_LOCALFS_TYPE=localfs
PIO_STORAGE_SOURCES_LOCALFS_HOSTS=/**replace_w_your_path_to**/.pio_store/models
PIO_STORAGE_SOURCES_LOCALFS_PORTS=0
PIO_STORAGE_SOURCES_HBASE_TYPE=hbase
PIO_STORAGE_SOURCES_HBASE_HOSTS=0
PIO_STORAGE_SOURCES_HBASE_PORTS=0
```

NOTE: Remember to replace the path that start with `**replace`. `.pio_store`
typically locates at your user home directory `~/`.

![Config](/images/intellij/intellij-config.png)

![Config](/images/intellij/intellij-config-2.png)

Save and you can run or debug with the new configuration.
