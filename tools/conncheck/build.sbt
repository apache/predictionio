import xerial.sbt.Pack._

name := "conncheck"

scalariformSettings

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.0"

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map("conncheck" -> "io.prediction.tools.conncheck.ConnCheck")

packJvmOpts := Map("conncheck" -> Common.packCommonJvmOpts)
