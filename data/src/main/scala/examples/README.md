Temporary place for examples and tests
======================================

HBPeventsTest
```
$ cd $PIO_HOME
$ ./make-distribution.sh
$ $SPARK_HOME/bin/spark-submit \
  --class "io.prediction.data.storage.examples.HBPEventsTest" \
  --master local[4] \
  assembly/pio-assembly-0.8.2.jar <appId>
```

PBatchViewTest
```
$ cd $PIO_HOME
$ ./make-distribution.sh
$ $SPARK_HOME/bin/spark-submit \
  --class "io.prediction.data.storage.examples.PBatchViewTest" \
  --master local[4] \
  assembly/pio-assembly-0.8.2.jar <appId>
```

TestEvents
```
$ sbt/sbt "data/compile"
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.examples.TestEvents HB"
```


HBLEventsTest
```
$ sbt/sbt "data/compile"
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.examples.HBLEventsTest 2"
```
