import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "predictionio-scheduler"
    val appVersion      = "0.6.2"

    val appDependencies = Seq(
      "commons-io" % "commons-io" % "2.4",
      "io.prediction" %% "predictionio-commons" % "0.6.2",
      "mysql" % "mysql-connector-java" % "5.1.22",
      "org.clapper" %% "scalasti" % "1.0.0",
      "org.quartz-scheduler" % "quartz" % "2.1.7",
      "org.specs2" %% "specs2" % "1.14" % "test"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += (
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      )
    )

}
