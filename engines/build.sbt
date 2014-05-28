name := "pio-engines"

libraryDependencies ++= Seq(
  "org.clapper"       %% "grizzled-slf4j" % "1.0.2",
  "org.scala-saddle"  %% "saddle-core"    % "1.3.2",
  "org.scalanlp"      %% "breeze"         % "0.7",
  "org.scalanlp"      %% "breeze-natives" % "0.7",
  "org.scalanlp"       % "nak"            % "1.2.1",
  "org.json4s"        %% "json4s-native"  % "3.2.9")

addCompilerPlugin("org.scala-sbt.sxr" %% "sxr" % "0.3.0")

scalacOptions <<= (scalacOptions, scalaSource in Compile) map { (options, base) =>
  options :+ ("-P:sxr:base-directory:" + base.getAbsolutePath)
}
