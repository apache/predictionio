import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {
  lazy val root = Project(
    id = "predictionio-tools-settingsinit",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        // Map from program name -> Main class (full path)
        packMain := Map("settingsinit" -> "io.prediction.tools.settingsinit.SettingsInit")
        // Add custom settings here
      )
  )
}
