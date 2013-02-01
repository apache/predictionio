import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {
  lazy val root = Project(
    id = "predictionio-tools-users",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        // Map from program name -> Main class (full path)
        packMain := Map("users" -> "io.prediction.tools.users.Users")
        // Add custom settings here
      )
  )
}
