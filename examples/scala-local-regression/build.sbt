import AssemblyKeys._

assemblySettings

name := "example-scala-local-regression"

organization := "io.prediction"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.prediction"     %% "core"           % "0.8.0-SNAPSHOT" % "provided",
  "com.github.scopt"  %% "scopt"          % "3.2.0",
  "commons-io"         % "commons-io"     % "2.4",
  "org.apache.spark"  %% "spark-core"     % "1.0.1" % "provided",
  "org.clapper"       %% "grizzled-slf4j" % "1.0.2",
  "org.json4s"        %% "json4s-native"  % "3.2.6",
  "org.scalanlp"       % "nak"            % "1.2.1")

//mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
//  {
//    case PathList("scala", xs @ _*) => MergeStrategy.discard
//    case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.last
//    case x => old(x)
//  }
//}
