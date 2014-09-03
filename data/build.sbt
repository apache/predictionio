name := "data"

version := "0.8-SNAPSHOT"

organization := "io.prediction"

scalaVersion := "2.10.4"

// spray-can, Scala 2.10 + Akka 2.2 + spray 1.2 (the on_spray-can_1.2 branch)
// spray-can, Scala 2.10 + Akka 2.3 + spray 1.3 (the on_spray-can_1.3 branch)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "io.spray"                % "spray-can"       % "1.3.1",
  "io.spray"                % "spray-routing"   % "1.3.1",
  "org.json4s"             %% "json4s-native"   % "3.2.10",
  "org.json4s"             %% "json4s-ext"      % "3.2.10",
  "org.clapper"            %% "grizzled-slf4j"  % "1.0.2",
  "org.elasticsearch"       % "elasticsearch"   % "1.2.1",
  "org.mongodb"            %% "casbah"          % "2.7.2",
  "org.apache.hadoop"       % "hadoop-common"  % "2.4.1",
  "org.apache.hbase" % "hbase-common" % "0.98.5-hadoop2",
  "org.apache.hbase" % "hbase-client" % "0.98.5-hadoop2")

  //"org.scala-lang" %% "scala-pickling" % "0.8.0")

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"
