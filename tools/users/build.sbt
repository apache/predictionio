import xerial.sbt.Pack._

name := "users"

scalariformSettings

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.8",
  "jline" % "jline" % "2.9",
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map("users" -> "io.prediction.tools.users.Users")

packJvmOpts := Map("users" -> Common.packCommonJvmOpts)
