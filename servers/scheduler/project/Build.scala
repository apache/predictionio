import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "predictionio-scheduler"
    val appVersion      = "0.3-SNAPSHOT"

    val appDependencies = Seq(
      "commons-io" % "commons-io" % "2.4",
      "io.prediction" %% "predictionio-commons" % "0.3-SNAPSHOT",
      "mysql" % "mysql-connector-java" % "5.1.22",
      "org.clapper" %% "scalasti" % "1.0.0",
      "org.quartz-scheduler" % "quartz" % "2.1.6",
      "org.specs2" %% "specs2" % "1.14" % "test"
    )

    /**
    val hadoopClasspath: Classpath = Seq(
      new File("/opt/mapr/hadoop/hadoop-0.20.2/conf"),
      new File("/opt/mapr/hadoop/hadoop-0.20.2/lib/hadoop-0.20.2-dev-core.jar"),
      new File("/opt/mapr/hadoop/hadoop-0.20.2/lib/commons-logging-1.0.4.jar"),
      new File("/opt/mapr/hadoop/hadoop-0.20.2/lib/log4j-1.2.15.jar"),
      new File("/opt/mapr/hadoop/hadoop-0.20.2/lib/maprfs-0.1.jar"),
      new File("/opt/mapr/hadoop/hadoop-0.20.2/lib/zookeeper-3.3.6.jar")).classpath
      */

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += (
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      )
      //unmanagedClasspath in Compile := hadoopClasspath
    )

}
