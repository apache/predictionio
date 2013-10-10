
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
  processItemRecAlgoScalaMahout,
  processItemSimAlgoHadoopScalding,
  toolsConncheck,
  toolsSettingsInit,
  toolsSoftwareManager,
  toolsUsers)

// Commons and Output

lazy val commons = project in file("commons")

lazy val output = project.in(file("output")).dependsOn(commons)

// Process Assemblies

lazy val processCommonsHadoopScalding = project
  .in(file("process/commons/hadoop/scalding")).dependsOn(commons)

lazy val processItemRecAlgoHadoopScalding = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding"))
  .aggregate(
    processItemRecAlgoHadoopScaldingGeneric,
    processItemRecAlgoHadoopScaldingKnnitembased,
    processItemRecAlgoHadoopScaldingRandomrank,
    processItemRecAlgoHadoopScaldingLatestrank,
    processItemRecAlgoHadoopScaldingMahout)
  .dependsOn(
    processItemRecAlgoHadoopScaldingGeneric,
    processItemRecAlgoHadoopScaldingKnnitembased,
    processItemRecAlgoHadoopScaldingRandomrank,
    processItemRecAlgoHadoopScaldingLatestrank,
    processItemRecAlgoHadoopScaldingMahout)

lazy val processItemRecAlgoHadoopScaldingGeneric = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/generic"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingKnnitembased = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/knnitembased"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingRandomrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/randomrank"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingLatestrank = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/latestrank"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemRecAlgoHadoopScaldingMahout = project
  .in(file("process/engines/itemrec/algorithms/hadoop/scalding/mahout"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemRecAlgoScalaMahout = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout"))
  .aggregate(
    processItemRecAlgoScalaMahoutCommons,
    processItemRecAlgoScalaMahoutALSWR,
    processItemRecAlgoScalaMahoutKNNUserBased,
    processItemRecAlgoScalaMahoutSlopeOne,
    processItemRecAlgoScalaMahoutSVDPlusPlus,
    processItemRecAlgoScalaMahoutSVDSGD,
    processItemRecAlgoScalaMahoutThresholdUserBased)
  .dependsOn(
    processItemRecAlgoScalaMahoutCommons,
    processItemRecAlgoScalaMahoutALSWR,
    processItemRecAlgoScalaMahoutKNNUserBased,
    processItemRecAlgoScalaMahoutSlopeOne,
    processItemRecAlgoScalaMahoutSVDPlusPlus,
    processItemRecAlgoScalaMahoutSVDSGD,
    processItemRecAlgoScalaMahoutThresholdUserBased)

lazy val processItemRecAlgoScalaMahoutCommons = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/commons"))
  .dependsOn(commons)

lazy val processItemRecAlgoScalaMahoutALSWR = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/alswr"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemRecAlgoScalaMahoutKNNUserBased = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/knnuserbased"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemRecAlgoScalaMahoutSlopeOne = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/slopeone"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemRecAlgoScalaMahoutSVDPlusPlus = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/svdplusplus"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemRecAlgoScalaMahoutSVDSGD = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/svdsgd"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemRecAlgoScalaMahoutThresholdUserBased = project
  .in(file("process/engines/itemrec/algorithms/scala/mahout/thresholduserbased"))
  .dependsOn(processItemRecAlgoScalaMahoutCommons)

lazy val processItemSimAlgoHadoopScalding = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding"))
  .aggregate(
    processItemSimAlgoHadoopScaldingItemSimCF,
    processItemSimAlgoHadoopScaldingLatestRank,
    processItemSimAlgoHadoopScaldingMahout,
    processItemSimAlgoHadoopScaldingRandomRank)
  .dependsOn(
    processItemSimAlgoHadoopScaldingItemSimCF,
    processItemSimAlgoHadoopScaldingLatestRank,
    processItemSimAlgoHadoopScaldingMahout,
    processItemSimAlgoHadoopScaldingRandomRank)

lazy val processItemSimAlgoHadoopScaldingItemSimCF = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/itemsimcf"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemSimAlgoHadoopScaldingLatestRank = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/latestrank"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemSimAlgoHadoopScaldingMahout = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/mahout"))
  .dependsOn(processCommonsHadoopScalding)

lazy val processItemSimAlgoHadoopScaldingRandomRank = project
  .in(file("process/engines/itemsim/algorithms/hadoop/scalding/randomrank"))
  .dependsOn(processCommonsHadoopScalding)

// Tools Section

lazy val toolsConncheck = project.in(file("tools/conncheck"))
  .dependsOn(commons)

lazy val toolsSettingsInit = project.in(file("tools/settingsinit"))
  .dependsOn(commons)

lazy val toolsSoftwareManager = project.in(file("tools/softwaremanager"))
  .dependsOn(commons)

lazy val toolsUsers = project.in(file("tools/users"))
  .dependsOn(commons)
