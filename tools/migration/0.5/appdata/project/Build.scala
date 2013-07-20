import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {
  lazy val root = Project(
    id = "predictionio-tools-migration-appdata",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        // Map from program name -> Main class (full path)
        packMain := Map("appdata" -> "io.prediction.tools.migration.Appdata")
        // Add custom settings here
      )
  )
}
