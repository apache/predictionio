import AssemblyKeys._

assemblySettings

name := "example-scala-elasticsearch"

organization := "io.prediction"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"

libraryDependencies ++= Seq(
  "io.prediction"    %% "core"          % "0.8.0-SNAPSHOT" % "provided",
  "commons-io"        % "commons-io"    % "2.4",
  "org.apache.spark" %% "spark-core"    % "1.0.2" % "provided",
  "org.apache.spark" %% "spark-mllib"   % "1.0.2" % "provided",
  "org.json4s"       %% "json4s-native" % "3.2.6",
  "org.elasticsearch" % "elasticsearch-hadoop" % "2.1.0.Beta1" 
    exclude("com.esotericsoftware.minlog", "minlog")
  )

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", "ECLIPSEF.RSA")         => MergeStrategy.discard
    case PathList("META-INF", "jboss-beans.xml")         => MergeStrategy.discard
    case PathList("META-INF", "mailcap")         => MergeStrategy.discard
    case PathList("META-INF", "maven", xs @ _*)         => MergeStrategy.discard
    case PathList("META-INF", "mimetypes.default")         => MergeStrategy.discard
    case PathList("com", "google", "common", xs @ _*) => MergeStrategy.last
    case PathList("com", "sun", "mail", xs @ _*) => MergeStrategy.last
    case PathList("images", "ant_logo_large.gif") => MergeStrategy.last
    case PathList("javax", xs @ _*) => MergeStrategy.last
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("org", "codehaus", xs @ _*) => MergeStrategy.last
    case PathList("org", "eclipse", xs @ _*) => MergeStrategy.last
    case PathList("org", "jboss", xs @ _*) => MergeStrategy.last
    case PathList("overview.html") => MergeStrategy.last
    case PathList("plugin.properties") => MergeStrategy.last
    case PathList("plugin.xml") => MergeStrategy.last
    case x => old(x)
  }
}
