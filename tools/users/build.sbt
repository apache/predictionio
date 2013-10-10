import xerial.sbt.Pack._

name := "predictionio-users-tool"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.8",
  "jline" % "jline" % "2.9"
)

packSettings

packMain := Map("users" -> "io.prediction.tools.users.Users")
