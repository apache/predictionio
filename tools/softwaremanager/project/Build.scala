import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "softwaremanager",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
    Seq(
      // Specify mappings from program name -> Main class (full package path)
      packMain := Map(
        "backup"      -> "io.prediction.tools.softwaremanager.Backup",
        "updatecheck" -> "io.prediction.tools.softwaremanager.UpdateCheck")
      // Add custom settings here
      // [Optional] JVM options of scripts (program name -> Seq(JVM option, ...))
      // packJvmOpts := Map("hello" -> Seq("-Xmx512m")),
      // [Optional] Extra class paths to look when launching a program
      // packExtraClasspath := Map("hello" -> Seq("${PROG_HOME}/etc"))
    )
  )
}
