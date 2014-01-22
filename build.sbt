import com.typesafe.sbt.SbtNativePackager.Universal

import com.typesafe.sbt.packager.Keys._

name := "predictionio"

version in ThisBuild := "0.6.7"

organization in ThisBuild := "io.prediction"

scalaVersion in ThisBuild := "2.10.2"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions in (ThisBuild, Test) ++= Seq("-Yrangepos")

javacOptions in ThisBuild ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:deprecation", "-Xlint:unchecked")

libraryDependencies in ThisBuild ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "0.6.0",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "test",
  "org.specs2" %% "specs2" % "1.14" % "test")

publishTo in ThisBuild := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle in ThisBuild := true

parallelExecution in (ThisBuild, Test) := false

lazy val root = project.in(file(".")).aggregate(
  commons,
  output,
  processHadoopScalding,
  processEnginesCommonsEvalScalaParamGen,
  processEnginesCommonsEvalScalaU2ITrainingTestSplit,
  processEnginesItemRecAlgoScalaMahout,
  processEnginesItemRecEvalScalaTopKItems,
  processEnginesItemSimEvalScalaTopKItems,
  toolsConncheck,
  toolsSettingsInit,
  toolsSoftwareManager,
  toolsUsers)

// Commons and Output

lazy val commons = project in file("commons") settings(scalariformSettings: _*)

lazy val output = project.in(file("output")).dependsOn(commons).settings(scalariformSettings: _*)

// Process Assemblies

lazy val processHadoopScalding = project
  .in(file("process"))
  .aggregate(
    processCommonsHadoopScalding,
    processEnginesCommonsEvalHadoopScalding,
    processEnginesItemRecAlgoHadoopScalding,
    processEnginesItemRecEvalHadoopScalding,
    processEnginesItemSimAlgoHadoopScalding,
    processEnginesItemSimEvalHadoopScalding)
  .dependsOn(
    processCommonsHadoopScalding,
    processEnginesCommonsEvalHadoopScalding,
    processEnginesItemRecAlgoHadoopScalding,
    processEnginesItemRecEvalHadoopScalding,
    processEnginesItemSimAlgoHadoopScalding,
    processEnginesItemSimEvalHadoopScalding)

lazy val processCommonsHadoopScalding = project
  .in(file("process/commons/hadoop/scalding")).dependsOn(commons)
  .settings(scalariformSettings: _*)

lazy val processEnginesCommonsEvalHadoopScalding = project
  .in(file("process/engines/commons/evaluations/hadoop/scalding"))
  .aggregate(processEnginesCommonsEvalHadoopScaldingU2ITrainingTestSplit)
  .dependsOn(processEnginesCommonsEvalHadoopScaldingU2ITrainingTestSplit)

lazy val processEnginesCommonsEvalHadoopScaldingU2ITrainingTestSplit = project
  .in(file("process/engines/commons/evaluations/hadoop/scalding/u2itrainingtestsplit"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesCommonsEvalScalaParamGen = project
  .in(file("process/engines/commons/evaluations/scala/paramgen"))
  .dependsOn(commons)
  .settings(scalariformSettings: _*)

lazy val processEnginesCommonsEvalScalaU2ITrainingTestSplit = project
  .in(file("process/engines/commons/evaluations/scala/u2itrainingtestsplit"))
  .dependsOn(commons)

lazy val processEnginesItemRecAlgoHadoopScalding = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding"))
  .aggregate(
    processEnginesItemRecAlgoHadoopScaldingGeneric,
    processEnginesItemRecAlgoHadoopScaldingKnnitembased,
    processEnginesItemRecAlgoHadoopScaldingRandomrank,
    processEnginesItemRecAlgoHadoopScaldingLatestrank,
    processEnginesItemRecAlgoHadoopScaldingMahout)
  .dependsOn(
    processEnginesItemRecAlgoHadoopScaldingGeneric,
    processEnginesItemRecAlgoHadoopScaldingKnnitembased,
    processEnginesItemRecAlgoHadoopScaldingRandomrank,
    processEnginesItemRecAlgoHadoopScaldingLatestrank,
    processEnginesItemRecAlgoHadoopScaldingMahout)

lazy val processEnginesItemRecAlgoHadoopScaldingGeneric = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/generic"))
  .dependsOn(processCommonsHadoopScalding)
  .settings(scalariformSettings: _*)

lazy val processEnginesItemRecAlgoHadoopScaldingKnnitembased = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/knnitembased"))
  .dependsOn(processCommonsHadoopScalding)
  .settings(scalariformSettings: _*)

lazy val processEnginesItemRecAlgoHadoopScaldingRandomrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/randomrank"))
  .dependsOn(processCommonsHadoopScalding)
  .settings(scalariformSettings: _*)

lazy val processEnginesItemRecAlgoHadoopScaldingLatestrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/latestrank"))
  .dependsOn(processCommonsHadoopScalding)
  .settings(scalariformSettings: _*)

lazy val processEnginesItemRecAlgoHadoopScaldingMahout = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/mahout"))
  .dependsOn(processCommonsHadoopScalding)
  .settings(scalariformSettings: _*)

lazy val processEnginesItemRecAlgoScalaMahout = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout"))
  .aggregate(
    processEnginesItemRecAlgoScalaMahoutCommons,
    processEnginesItemRecAlgoScalaMahoutALSWR,
    processEnginesItemRecAlgoScalaMahoutKNNUserBased,
    processEnginesItemRecAlgoScalaMahoutSlopeOne,
    processEnginesItemRecAlgoScalaMahoutSVDPlusPlus,
    processEnginesItemRecAlgoScalaMahoutSVDSGD,
    processEnginesItemRecAlgoScalaMahoutThresholdUserBased)
  .dependsOn(
    processEnginesItemRecAlgoScalaMahoutCommons,
    processEnginesItemRecAlgoScalaMahoutALSWR,
    processEnginesItemRecAlgoScalaMahoutKNNUserBased,
    processEnginesItemRecAlgoScalaMahoutSlopeOne,
    processEnginesItemRecAlgoScalaMahoutSVDPlusPlus,
    processEnginesItemRecAlgoScalaMahoutSVDSGD,
    processEnginesItemRecAlgoScalaMahoutThresholdUserBased)

lazy val processEnginesItemRecAlgoScalaMahoutCommons = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/commons"))
  .dependsOn(commons)

lazy val processEnginesItemRecAlgoScalaMahoutALSWR = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/alswr"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecAlgoScalaMahoutKNNUserBased = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/knnuserbased"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecAlgoScalaMahoutSlopeOne = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/slopeone"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecAlgoScalaMahoutSVDPlusPlus = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/svdplusplus"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecAlgoScalaMahoutSVDSGD = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/svdsgd"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecAlgoScalaMahoutThresholdUserBased = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/thresholduserbased"))
  .dependsOn(processEnginesItemRecAlgoScalaMahoutCommons)

lazy val processEnginesItemRecEvalHadoopScalding = project
  .in(file("process/engines/itemrec/evaluations/hadoop/scalding"))
  .aggregate(
    processEnginesItemRecEvalHadoopScaldingMetricsMAP)
  .dependsOn(
    processEnginesItemRecEvalHadoopScaldingMetricsMAP)

lazy val processEnginesItemRecEvalHadoopScaldingMetricsMAP = project
  .in(file("process/engines/itemrec/evaluations/hadoop/scalding/metrics/map"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemRecEvalScalaTopKItems = project
  .in(file("process/engines/itemrec/evaluations/scala/topkitems"))
  .dependsOn(commons, output)

lazy val processEnginesItemSimAlgoHadoopScalding = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding"))
  .aggregate(
    processEnginesItemSimAlgoHadoopScaldingItemSimCF,
    processEnginesItemSimAlgoHadoopScaldingLatestRank,
    processEnginesItemSimAlgoHadoopScaldingMahout,
    processEnginesItemSimAlgoHadoopScaldingRandomRank)
  .dependsOn(
    processEnginesItemSimAlgoHadoopScaldingItemSimCF,
    processEnginesItemSimAlgoHadoopScaldingLatestRank,
    processEnginesItemSimAlgoHadoopScaldingMahout,
    processEnginesItemSimAlgoHadoopScaldingRandomRank)

lazy val processEnginesItemSimAlgoHadoopScaldingItemSimCF = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/itemsimcf"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemSimAlgoHadoopScaldingLatestRank = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/latestrank"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemSimAlgoHadoopScaldingMahout = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/mahout"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemSimAlgoHadoopScaldingRandomRank = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/randomrank"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemSimEvalHadoopScalding = project
  .in(file("process/engines/itemsim/evaluations/hadoop/scalding"))
  .aggregate(
    processEnginesItemSimEvalHadoopScaldingMetricsISMAP)
  .dependsOn(
    processEnginesItemSimEvalHadoopScaldingMetricsISMAP)

lazy val processEnginesItemSimEvalHadoopScaldingMetricsISMAP = project
  .in(file("process/engines/itemsim/evaluations/hadoop/scalding/metrics/ismap"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processEnginesItemSimEvalScalaTopKItems = project
  .in(file("process/engines/itemsim/evaluations/scala/topkitems"))
  .dependsOn(commons, output)

// Tools Section

lazy val toolsConncheck = project.in(file("tools/conncheck"))
  .dependsOn(commons)

lazy val toolsSettingsInit = project.in(file("tools/settingsinit"))
  .dependsOn(commons)

lazy val toolsSoftwareManager = project.in(file("tools/softwaremanager"))
  .dependsOn(commons)

lazy val toolsUsers = project.in(file("tools/users"))
  .dependsOn(commons)
