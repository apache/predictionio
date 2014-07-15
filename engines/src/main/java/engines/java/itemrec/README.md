# Mahout ItemRec Engine

* data/ - engine data
* algos/ - algo


## Run

Create Sample data at /tmp/pio/ratings.csv.

Then, at project root (Imagine/):

    $ sbt/sbt package
    $ sbt/sbt "project engines" assemblyPackageDependency
    $ $SPARK_HOME/bin/spark-submit --jars  engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar --deploy-mode "client" --class "io.prediction.engines.java.itemrec.Runner"  core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar
