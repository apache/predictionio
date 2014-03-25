import xerial.sbt.Pack._

name := "standardized-info-ids"

scalariformSettings

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "ch.qos.logback" % "logback-core" % "1.1.1",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map("standardized-info-ids" -> "io.prediction.tools.migration.StandardizedInfoIDs")

packJvmOpts := Map("standardized-info-ids" -> Common.packCommonJvmOpts)
