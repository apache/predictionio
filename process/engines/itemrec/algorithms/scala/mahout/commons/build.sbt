name := "PredictionIO-Process-ItemRec-Algorithms-Scala-Mahout-Commons"

version := "0.3-SNAPSHOT"

scalaVersion := "2.10.0"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.3-SNAPSHOT"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

resolvers += "Clojars Repository" at "http://clojars.org/repo"
