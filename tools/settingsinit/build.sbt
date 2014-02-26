import xerial.sbt.Pack._

name := "settingsinit"

scalariformSettings

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.6.0"

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map("settingsinit" -> "io.prediction.tools.settingsinit.SettingsInit")

packJvmOpts := Map("settingsinit" -> Common.packCommonJvmOpts)
