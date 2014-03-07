import xerial.sbt.Pack._

name := "predictionio-process-itemrec-algorithms-scala-mahout"

libraryDependencies ++= Seq(
  "org.apache.mahout" % "mahout-core" % "0.9",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.twitter" %% "scalding-args" % "0.8.11",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1")

parallelExecution in Test := false

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemrec.mahout.mahoutjob" -> "io.prediction.algorithms.mahout.itemrec.MahoutJob",
  "itemrec.mahout.modelcon" -> "io.prediction.algorithms.mahout.itemrec.MahoutModelConstructor")

packJvmOpts := Map(
  "itemrec.mahout.mahoutjob" -> Common.packCommonJvmOpts,
  "itemrec.mahout.modelcon" -> Common.packCommonJvmOpts)
