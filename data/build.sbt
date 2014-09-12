name := "data"

libraryDependencies ++= Seq(
  "com.google.guava"        % "guava"           % "18.0",
  "io.spray"                % "spray-can"       % "1.2.1",
  "io.spray"                % "spray-routing"   % "1.2.1",
  "org.apache.hadoop"       % "hadoop-common"   % "2.5.0",
  "org.apache.hbase"        % "hbase-common"    % "0.98.5-hadoop2",
  "org.apache.hbase"        % "hbase-client"    % "0.98.5-hadoop2",
  "org.clapper"            %% "grizzled-slf4j"  % "1.0.2",
  "org.elasticsearch"       % "elasticsearch"   % "1.2.1",
  "org.json4s"             %% "json4s-native"   % json4sVersion.value,
  "org.json4s"             %% "json4s-ext"      % json4sVersion.value,
  "org.mongodb"            %% "casbah"          % "2.7.2",
  "org.scalatest"          %% "scalatest"       % "2.1.6" % "test",
  "org.slf4j"               % "slf4j-log4j12"   % "1.7.7",
  "org.spark-project.akka" %% "akka-actor"      % "2.2.3-shaded-protobuf",
  "org.specs2"             %% "specs2"          % "2.3.13" % "test")
