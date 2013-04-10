import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {
  lazy val root = Project(
    id = "predictionio-tools-migration-settings04",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        // Map from program name -> Main class (full path)
        packMain := Map("settings04" -> "io.prediction.tools.migration.Settings04")
        // Add custom settings here
      )
  )
}
