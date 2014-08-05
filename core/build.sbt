name := "core"

libraryDependencies ++= Seq(
  "com.github.scopt"       %% "scopt"           % "3.2.0",
  "com.google.code.gson"    % "gson"            % "2.2.4",
  "com.google.guava"        % "guava"           % "17.0",
  "com.twitter"            %% "chill"           % "0.3.6"
    exclude("com.esotericsoftware.minlog", "minlog"),
  "com.twitter"            %% "chill-bijection" % "0.3.6",
  "commons-io"              % "commons-io"      % "2.4",
  "io.spray"                % "spray-can"       % "1.2.1",
  "io.spray"                % "spray-routing"   % "1.2.1",
  "net.jodah"               % "typetools"       % "0.3.1",
  "org.apache.spark"       %% "spark-core"      % "1.0.1" % "provided",
  "org.clapper"            %% "grizzled-slf4j"  % "1.0.2",
  "org.elasticsearch"       % "elasticsearch"   % "1.2.1",
  "org.json4s"             %% "json4s-native"   % "3.2.6",
  "org.json4s"             %% "json4s-ext"      % "3.2.6",
  "org.mongodb"            %% "casbah"          % "2.7.2",
  "org.scalatest"          %% "scalatest"       % "2.1.6" % "test",
  "org.slf4j"               % "slf4j-log4j12"   % "1.7.7",
  "org.specs2"             %% "specs2"          % "2.3.13" % "test")

net.virtualvoid.sbt.graph.Plugin.graphSettings
