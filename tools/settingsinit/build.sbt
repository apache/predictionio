import xerial.sbt.Pack._

name := "predictionio-settings-initialization"

packSettings

packMain := Map("settingsinit" -> "io.prediction.tools.settingsinit.SettingsInit")
