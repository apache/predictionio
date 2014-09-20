ItemRank engine
===============

Personalize the order of a list of items for each user.


## Register engine directly. Useful for testing after engine code change.
```
$ cd engines/
$ $PIO_HOME/bin/pio register --engine-json src/main/scala/itemrank/examples/engine.json
$ $PIO_HOME/bin/pio train --engine-json src/main/scala/itemrank/examples/engine.json --params-path src/main/scala/itemrank/examples/params -ap ncMahoutItemBasedAlgo.json
```

## Register engine through distribution. Useful for params testing.
```
$ ./make_distribtuion.sh
$ cd engines/src/main/scala/itemrank/examples/
$ $PIO_HOME/bin/pio register
$ $PIO_HOME/bin/pio train -ap ncMahoutItemBasedAlgo.json
```

Run Runner (obsolete)
==============
```
$ pushd engines
$ ../bin/pio-run  io.prediction.engines.itemrank.DetailedRunner
$ popd engines

$ ../bin/pio-run  io.prediction.engines.itemrank.Runner
```

Import Sample Data (obsolete)
==================
Run under examples.
```
examples$ set -a
examples$ source ../conf/pio-env.sh
examples$ set +a

examples$ ../sbt/sbt "runMain io.prediction.engines.itemrank.CreateSampleData"
```
