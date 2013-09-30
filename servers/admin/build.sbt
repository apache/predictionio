name := "predictionio-admin"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.8")

playAssetsDirectories <+= baseDirectory / "enginebase"

play.Project.playScalaSettings
