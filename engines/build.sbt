import AssemblyKeys._

assemblySettings

name := "engines"

libraryDependencies ++= Seq(
  "org.clapper"       %% "grizzled-slf4j" % "1.0.2",
  "org.scala-saddle"  %% "saddle-core"    % "1.3.2",
  "org.scalanlp"      %% "breeze"         % "0.7",
  "org.scalanlp"      %% "breeze-natives" % "0.7",
  "org.scalanlp"       % "nak"            % "1.2.1",
  //"org.json4s"        %% "json4s-native"  % "3.2.9",
  "org.json4s"        %% "json4s-native"  % "3.2.10",
  "com.github.scopt"  %% "scopt"          % "3.2.0",
  "org.apache.spark"  %% "spark-core"      % "1.0.0" % "provided",
  "org.apache.spark"  %% "spark-mllib"      % "1.0.0" % "provided",
  "org.apache.mahout" % "mahout-core" % "0.9"
  )

addCompilerPlugin("org.scala-sbt.sxr" %% "sxr" % "0.3.0")

scalacOptions <<= (scalacOptions, scalaSource in Compile) map { (options, base) =>
  options :+ ("-P:sxr:base-directory:" + base.getAbsolutePath)
}

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// use case s if s.endsWith(".class") => MergeStrategy.last due to the folowing:
//[error] (engines/*:assemblyPackageDependency) deduplicate: different file contents found in the following:
//[error] /Users/ckh/.ivy2/cache/xmlpull/xmlpull/jars/xmlpull-1.1.3.1.jar:org/xmlpull/v1/XmlPullParser.class
//[error] /Users/ckh/.ivy2/cache/xpp3/xpp3_min/jars/xpp3_min-1.1.4c.jar:org/xmlpull/v1/XmlPullParser.class

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("scala", xs @ _*) => MergeStrategy.discard
    case s if s.endsWith(".class") => MergeStrategy.last
    case x => old(x)
  }
}
