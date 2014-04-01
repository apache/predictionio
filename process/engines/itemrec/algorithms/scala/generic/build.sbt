import xerial.sbt.Pack._

name := "predictionio-process-itemrec-algorithms-scala-generic"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.twitter" %% "scalding-args" % "0.8.11",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemrec.generic.dataprep" -> "io.prediction.algorithms.generic.itemrec.GenericDataPreparator")

packJvmOpts := Map(
  "itemrec.generic.dataprep" -> Common.packCommonJvmOpts)
