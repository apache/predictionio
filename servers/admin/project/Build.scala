import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "predictionio-admin"
    val appVersion      = "0.4.3-SNAPSHOT"

    val appDependencies = Seq(
      "io.prediction" %% "predictionio-commons" % "0.4.3-SNAPSHOT",
      "io.prediction" %% "predictionio-output" % "0.4.3-SNAPSHOT",
      "com.github.nscala-time" %% "nscala-time" % "0.2.0"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      playAssetsDirectories <+= baseDirectory / "enginebase",
      resolvers += (
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      ),
      scalacOptions ++= Seq("-deprecation","-unchecked","-feature")
    )

}
