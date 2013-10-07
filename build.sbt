
name := "predictionio"

version in ThisBuild := "0.7.0-SNAPSHOT"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.10.2"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies in ThisBuild ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "0.6.0")

publishTo in ThisBuild := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle in ThisBuild := true

lazy val root = project.in(file(".")).aggregate(
  commons,
  output,
  processCommonsHadoopScalding,
  processItemRecAlgoHadoopScalding,
  toolsConncheck,
  toolsSoftwareManager)
  //admin,
  //api,
  //scheduler)

lazy val commons = project in file("commons")

lazy val output = project.in(file("output")).dependsOn(commons)

lazy val processCommonsHadoopScalding = project
  .in(file("process/commons/hadoop/scalding")).dependsOn(commons)

lazy val processItemRecAlgoHadoopScalding = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding")).aggregate(
    processItemRecAlgoHadoopScaldingGeneric,
    processItemRecAlgoHadoopScaldingKnnitembased,
    processItemRecAlgoHadoopScaldingRandomrank,
    processItemRecAlgoHadoopScaldingLatestrank,
    processItemRecAlgoHadoopScaldingMahout)
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingGeneric = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/generic"))
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingKnnitembased = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/knnitembased"))
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingRandomrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/randomrank"))
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingLatestrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/latestrank"))
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingMahout = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/mahout"))
  .dependsOn(commons, processCommonsHadoopScalding)

lazy val toolsConncheck = project.in(file("tools/conncheck"))
  .dependsOn(commons)

lazy val toolsSoftwareManager = project.in(file("tools/softwaremanager"))
  .dependsOn(commons)

//lazy val admin = project.in(file("servers/admin"))
//	.dependsOn(commons, output)

//lazy val api = project.in(file("servers/api"))
//	.dependsOn(commons, output)

//lazy val scheduler = project.in(file("servers/scheduler"))
//	.dependsOn(commons)
