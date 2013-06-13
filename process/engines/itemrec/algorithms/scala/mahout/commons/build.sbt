name := "PredictionIO-Process-ItemRec-Algorithms-Scala-Mahout-Commons"

libraryDependencies ++= Seq(
  "io.prediction" %% "predictionio-commons" % "0.4.3-SNAPSHOT"
)

// Mahout's dependencies
libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "13.0.1",
  "org.codehaus.jackson" % "jackson-core-asl" % "1.9.11",
  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.11",
  "org.slf4j" % "slf4j-api" % "1.7.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-io" % "commons-io" % "2.4",
  "com.thoughtworks.xstream" % "xstream" % "1.4.4",
  "org.apache.lucene" % "lucene-core" % "4.2.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.2.0",
  "org.apache.mahout.commons" % "commons-cli" % "2.0-mahout",
  "org.apache.commons" % "commons-math3" % "3.2"
)
