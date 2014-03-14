import xerial.sbt.Pack._

name := "predictionio-process-itemsim-algorithms-scala-mahout"

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
  "itemsim.mahout.mahoutjob" -> "io.prediction.algorithms.mahout.itemsim.MahoutJob",
  "itemsim.mahout.modelcon" -> "io.prediction.algorithms.mahout.itemsim.MahoutModelConstructor")

packJvmOpts := Map(
  "itemsim.mahout.mahoutjob" -> Common.packCommonJvmOpts,
  "itemsim.mahout.modelcon" -> Common.packCommonJvmOpts)
