import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "PredictionIO-Output-API"
  val appVersion      = "0.3-SNAPSHOT"

  val appDependencies = Seq(
    "io.prediction" %% "predictionio-commons" % "0.3-SNAPSHOT",
    "io.prediction" %% "predictionio-output" % "0.3-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += (
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    )
  )

}
