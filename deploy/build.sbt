name := "deploy"

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-classic" % "1.1.2",
  "com.github.scopt"  %% "scopt"           % "3.2.0",
  "com.twitter"       %% "chill"           % "0.3.6",
  "com.twitter"       %% "chill-bijection" % "0.3.6",
  "com.typesafe"       % "config"          % "1.2.1",
  "com.typesafe.akka" %% "akka-actor"      % "2.3.3",
  "com.typesafe.akka" %% "akka-cluster"    % "2.3.3",
  "com.typesafe.akka" %% "akka-contrib"    % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit"    % "2.3.3",
  "io.spray"           % "spray-can"       % "1.3.1",
  "io.spray"           % "spray-routing"   % "1.3.1",
  "org.apache.spark"  %% "spark-core"      % "1.0.0"
    exclude("org.spark-project.akka", "akka-actor_2.10")
    exclude("org.spark-project.akka", "akka-remote_2.10")
    exclude("org.spark-project.akka", "akka-slf4j_2.10"),
  "org.apache.spark"  %% "spark-mllib"     % "1.0.0",
  "org.clapper"       %% "grizzled-slf4j"  % "1.0.2",
  "org.mongodb"       %% "casbah"          % "2.7.2",
  "org.json4s"        %% "json4s-native"   % "3.2.6",
  "org.json4s"        %% "json4s-ext"      % "3.2.6")

packSettings

packMain := Map("runserver" -> "io.prediction.deploy.RunServer")
