Running Evaluation
==================

The following is a regression example:

- Make sure Elasticsearch is running at localhost:9300, or MongoDB is running at localhost:27017.
- You only need to run RegisterEngine once unless you updated your engine's manifest.

First, copy ``conf/pio-env.sh.template`` to ``conf/pio-env.sh``. If you use MongoDB, add these:

```
PIO_STORAGE_SOURCES_MONGODB_TYPE=mongodb
PIO_STORAGE_SOURCES_MONGODB_HOSTS=localhost
PIO_STORAGE_SOURCES_MONGODB_PORTS=27017
```

and change these lines

```
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=ELASTICSEARCH
PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=ELASTICSEARCH
```

to

```
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=MONGODB
PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=MONGODB
```

Once your data store is properly configured, build binaries:

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
sbt/sbt tools/assembly
```


Pure Scala Workflow
-------------------

```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/scala/regression/examples/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/examples
```


Pure Java Workflow
------------------

```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/java/regression/examples/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.engines.java.regression.MeanSquareMetrics
```


### Using a Scala Metrics

Replace the last line in the section above with the following:

```
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.controller.MeanSquareError
```
