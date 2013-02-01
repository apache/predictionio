import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "PredictionIO Admin Server"
    val appVersion      = "0.1-SNAPSHOT"

    val appDependencies = Seq(
      "io.prediction" %% "predictionio-commons" % "0.1-SNAPSHOT" changing()
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += (
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      )
    )

}
