import AssemblyKeys._

assemblySettings

name := "pio-spark"

libraryDependencies ++= Seq(
  "org.clapper"       %% "grizzled-slf4j" % "1.0.2",
  "org.scala-saddle"  %% "saddle-core"    % "1.3.2",
  "org.scalanlp"      %% "breeze"         % "0.7",
  "org.scalanlp"      %% "breeze-natives" % "0.7",
  "org.scalanlp"       % "nak"            % "1.2.1",
  "org.json4s"        %% "json4s-native"  % "3.2.9",
  "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided",
  "com.typesafe"       % "config"          % "1.2.1",
  "com.typesafe.akka" %% "akka-contrib"    % "2.3.2",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.2",
  "org.clapper"       %% "grizzled-slf4j"  % "1.0.2",
  "com.github.scopt"  %% "scopt"          % "3.2.0")
  
// "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided",

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += Resolver.url(
  "Typesafe Releases",
    url("http://repo.typesafe.com/typesafe/ivy-releases"))(
        Resolver.ivyStylePatterns)


addCompilerPlugin("org.scala-sbt.sxr" %% "sxr" % "0.3.0")

scalacOptions <<= (scalacOptions, scalaSource in Compile) map { (options, base) =>
  options :+ ("-P:sxr:base-directory:" + base.getAbsolutePath)
}

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("scala", xs @ _*) => MergeStrategy.discard
    case x => old(x)
  }
}
