import xerial.sbt.Pack._

name := "softwaremanager"

scalariformSettings

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.1.0",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-nop" % "1.6.0")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "backup"      -> "io.prediction.tools.softwaremanager.Backup",
  "restore"     -> "io.prediction.tools.softwaremanager.Restore",
  "updatecheck" -> "io.prediction.tools.softwaremanager.UpdateCheck",
  "upgrade"     -> "io.prediction.tools.softwaremanager.Upgrade")

packJvmOpts := Map(
  "backup"      -> Common.packCommonJvmOpts,
  "restore"     -> Common.packCommonJvmOpts,
  "updatecheck" -> Common.packCommonJvmOpts,
  "upgrade"     -> Common.packCommonJvmOpts)
