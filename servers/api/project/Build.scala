import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "predictionio-api"
  val appVersion      = "0.6.3"

  val appDependencies = Seq(
    "io.prediction" %% "predictionio-commons" % "0.6.3",
    "io.prediction" %% "predictionio-output" % "0.6.3"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    javaOptions in Test += "-Dconfig.file=conf/test.conf",
    resolvers += (
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    )
  )

}
