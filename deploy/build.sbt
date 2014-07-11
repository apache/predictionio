import AssemblyKeys._

assemblySettings

name := "deploy"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster"    % "2.2.3",
  "com.typesafe.akka" %% "akka-contrib"    % "2.2.3",
  "io.spray"           % "spray-can"       % "1.2.1",
  "io.spray"           % "spray-routing"   % "1.2.1",
  "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided",
  "org.clapper"       %% "grizzled-slf4j"  % "1.0.2",
  "org.json4s"        %% "json4s-native"   % "3.2.6",
  "org.json4s"        %% "json4s-ext"      % "3.2.6")
