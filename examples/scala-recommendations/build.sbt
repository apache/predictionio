import AssemblyKeys._

assemblySettings

name := "example-scala-recommendations"

organization := "io.prediction"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.0-SNAPSHOT" % "provided",
  "commons-io"        % "commons-io"    % "2.4",
  "org.apache.spark" %% "spark-core"    % "1.0.2" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.0.2" % "provided",
    //exclude("org.apache.spark", "spark-core_2.10")
    //exclude("org.eclipse.jetty", "jetty-server"),
  "org.json4s"       %% "json4s-native" % "3.2.6")
