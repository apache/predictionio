# Mahout ItemRec Engine

* data/ - engine data
* algos/ - algo

## Compile and Run

Then, at project root (Imagine/):

    $ sbt/sbt package
    $ sbt/sbt "project engines" assemblyPackageDependency

Run GenericItemBased:

    $ $SPARK_HOME/bin/spark-submit --jars  engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar --deploy-mode "client" --class "io.prediction.examples.java.itemrec.Runner"  core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar

By default, the sample data engines/src/main/java/engines/java/itemrec/examples/ratings.csv is used. You may provide othere data source file.

## Use ml-100k Data Set

Download data set:

    $ curl http://files.grouplens.org/papers/ml-100k.zip -o ml-100k.zip
    $ unzip ml-100k.zip

**Note: because the algorithm will look at all other files in the same directory, the data source file directory must contain only valid data files.**

Since the ml-100k directory contain other files, copy the u.data for the reasons above:

    $ cp ml-100k/u.data  <your data source file directory>/


Run GenericItemBased with **\<your data source file directory\>/u.data**:

	$ $SPARK_HOME/bin/spark-submit --jars  engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar --deploy-mode "client" --class "io.prediction.examples.java.itemrec.Runner"  core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar <your data source file directory>/u.data genericitembased

Run SVDPlusPlus with **\<your data source file directory\>/u.data**:

    $ $SPARK_HOME/bin/spark-submit --jars  engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar --deploy-mode "client" --class "io.prediction.examples.java.itemrec.Runner"  core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar <your data source file directory>/u.data svdplusplus
