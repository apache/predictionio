import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "PredictionIO Output API"
  val appVersion      = "0.2-SNAPSHOT"

  val appDependencies = Seq(
    "io.prediction" %% "predictionio-commons" % "0.2-SNAPSHOT" changing(),
    "io.prediction" %% "predictionio-output" % "0.2-SNAPSHOT" changing()
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += (
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    )
  )

}
