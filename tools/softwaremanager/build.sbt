import xerial.sbt.Pack._

name := "predictionio-software-manager"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.1.0",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-nop" % "1.6.0",
  "org.specs2" %% "specs2" % "2.1.1" % "test"
)

packSettings

packMain := Map(
  "backup"      -> "io.prediction.tools.softwaremanager.Backup",
  "restore"     -> "io.prediction.tools.softwaremanager.Restore",
  "updatecheck" -> "io.prediction.tools.softwaremanager.UpdateCheck",
  "upgrade"     -> "io.prediction.tools.softwaremanager.Upgrade")
