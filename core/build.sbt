name := "core"

//"com.typesafe.akka" %% "akka-agent"    % "2.3.2",
//"com.typesafe.akka" %% "akka-cluster"    % "2.3.2",
//"com.typesafe.akka" %% "akka-remote"    % "2.3.2",

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic" % "1.1.2",
  "com.github.scopt"  %% "scopt"           % "3.2.0",
  "com.twitter"       %% "chill"           % "0.3.6" exclude("com.esotericsoftware.minlog", "minlog"),
  "com.twitter"       %% "chill-bijection" % "0.3.6",
  "com.typesafe"       % "config"          % "1.2.1",
  "com.typesafe.akka" %% "akka-actor"      % "2.3.2" % "provided",
  "com.typesafe.akka" %% "akka-contrib"    % "2.3.2" % "provided",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.2" % "provided",
  "com.typesafe.akka" %% "akka-slf4j"      % "2.3.2" % "provided",
  "commons-io"         % "commons-io"      % "2.4",
  "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided",
  "org.clapper"       %% "grizzled-slf4j"  % "1.0.2",
  "org.mongodb"       %% "casbah"          % "2.7.2",
  "org.scalatest"     %% "scalatest"       % "2.1.6" % "test",
  "org.json4s"        %% "json4s-native"   % "3.2.10",
  "org.json4s"        %% "json4s-jackson"  % "3.2.10",
  "org.json4s"        %% "json4s-ext"      % "3.2.10")

//scalacOptions <<= (scalacOptions, scalaSource in Compile) map { (options, base) =>
//  options :+ ("-P:sxr:base-directory:" + base.getAbsolutePath)
//}

//run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

resolvers += Resolver.url(
  "Typesafe Releases",
  url("http://repo.typesafe.com/typesafe/ivy-releases"))(
    Resolver.ivyStylePatterns)

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

//addCompilerPlugin("org.scala-sbt.sxr" %% "sxr" % "0.3.0")

net.virtualvoid.sbt.graph.Plugin.graphSettings
