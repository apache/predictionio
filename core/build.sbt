name := "core"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic" % "1.1.2",
  "com.twitter"       %% "chill"           % "0.3.6" exclude("com.esotericsoftware.minlog", "minlog"),
  "com.typesafe"       % "config"          % "1.2.1",
  "com.typesafe.akka" %% "akka-contrib"    % "2.3.2",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.2",
  "commons-io"         % "commons-io"      % "2.4",
  "org.clapper"       %% "grizzled-slf4j"  % "1.0.2",
  "org.mongodb"       %% "casbah"          % "2.7.2",
  "org.scalatest"     %% "scalatest"       % "2.1.6" % "test",
  "org.json4s"        %% "json4s-native"   % "3.2.9",
  "org.json4s"        %% "json4s-ext"      % "3.2.7",
  "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided")

scalacOptions <<= (scalacOptions, scalaSource in Compile) map { (options, base) =>
  options :+ ("-P:sxr:base-directory:" + base.getAbsolutePath)
}

resolvers += Resolver.url(
  "Typesafe Releases",
  url("http://repo.typesafe.com/typesafe/ivy-releases"))(
    Resolver.ivyStylePatterns)

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

addCompilerPlugin("org.scala-sbt.sxr" %% "sxr" % "0.3.0")
