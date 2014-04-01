import xerial.sbt.Pack._

name := "predictionio-process-itemsim-algorithms-scala-graphchi"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.twitter" %% "scalding-args" % "0.8.11",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1",
  "org.scalanlp" %% "breeze" % "0.6.1")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemsim.graphchi.modelcon" -> "io.prediction.algorithms.graphchi.itemsim.GraphChiModelConstructor")

packJvmOpts := Map(
  "itemsim.graphchi.modelcon" -> Common.packCommonJvmOpts)
