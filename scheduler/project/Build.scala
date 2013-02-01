import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "predictionio-scheduler"
    val appVersion      = "0.1"

    val appDependencies = Seq(
      "commons-io" % "commons-io" % "2.4",
      "io.prediction" %% "predictionio-commons" % "0.1-SNAPSHOT" changing(),
      "mysql" % "mysql-connector-java" % "5.1.22",
      "org.clapper" %% "scalasti" % "0.5.8",
      "org.quartz-scheduler" % "quartz" % "2.1.6",
      "org.specs2" %% "specs2" % "1.12.3" % "test"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += (
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      )
    )

}
