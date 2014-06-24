Register Engine

$ sbt/sbt "core/run ../engines/src/main/scala/stock/examples/manifest.json"
select "io.prediction.tools.RegisterEngine"

$ sbt/sbt "engines/assembly"

$ sbt/sbt "core/run io.prediction.engines.stock 0.8.0-SNAPSHOT --jsonDir ../engines/src/main/scala/stock/examples/"
