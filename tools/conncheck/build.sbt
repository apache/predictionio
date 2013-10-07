import xerial.sbt.Pack._

name := "predictionio-connection-check-tool"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-nop" % "1.6.0"
)

packSettings

packMain := Map("conncheck" -> "io.prediction.tools.conncheck.ConnCheck")
