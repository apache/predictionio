import xerial.sbt.Pack._

name := "predictionio-process-itemsim-algorithms-scala-generic"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.twitter" %% "scalding-args" % "0.8.11",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemsim.generic.dataprep" -> "io.prediction.algorithms.generic.itemsim.GenericDataPreparator")

packJvmOpts := Map(
  "itemsim.generic.dataprep" -> Common.packCommonJvmOpts)
