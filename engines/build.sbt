import AssemblyKeys._

assemblySettings

name := "engines"

organization := "io.prediction"

version := "0.8.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.prediction"     %% "core"           % "0.8.0-SNAPSHOT" % "provided",
  "com.github.scopt"  %% "scopt"          % "3.2.0",
  "commons-io"         % "commons-io"     % "2.4",
  "org.apache.commons" % "commons-math3"  % "3.3",
  "org.apache.mahout"  % "mahout-core"    % "0.9",
  "org.apache.spark"  %% "spark-core"     % "1.0.2" % "provided",
  "org.apache.spark"  %% "spark-mllib"    % "1.0.2"
    exclude("org.apache.spark", "spark-core_2.10")
    exclude("org.eclipse.jetty", "jetty-server"),
  "org.clapper"       %% "grizzled-slf4j" % "1.0.2",
  "org.json4s"        %% "json4s-native"  % "3.2.6",
  "org.scalatest"     %% "scalatest"      % "2.2.0" % "test")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("scala", xs @ _*) => MergeStrategy.discard
    case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.last
    case x => old(x)
  }
}

run in Compile <<= Defaults.runTask(
  fullClasspath in Compile,
  mainClass in (Compile, run),
  runner in (Compile, run))

runMain in Compile <<= Defaults.runMainTask(
  fullClasspath in Compile,
  runner in (Compile, run))

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)
