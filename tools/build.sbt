import AssemblyKeys._

assemblySettings

name := "tools"

libraryDependencies ++= Seq(
  "com.github.scopt"       %% "scopt"           % "3.2.0",
  "org.apache.hadoop"       % "hadoop-common"   % "2.4.1",
  "org.apache.hadoop"       % "hadoop-hdfs"     % "2.4.1",
  "org.clapper"            %% "grizzled-slf4j"  % "1.0.2",
  "org.json4s"             %% "json4s-native"   % "3.2.6",
  "org.json4s"             %% "json4s-ext"      % "3.2.6")

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter { _.data.getName match {
    case "asm-3.1.jar" => true
    case "commons-beanutils-1.7.0.jar" => true
    case "commons-beanutils-core-1.8.0.jar" => true
    //case "commons-collections-3.2.1.jar" => true
    case "slf4j-log4j12-1.7.5.jar" => true
    case _ => false
  }}
}

net.virtualvoid.sbt.graph.Plugin.graphSettings
