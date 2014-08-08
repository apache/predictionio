import AssemblyKeys._

assemblySettings

name := "example-scala-local-regression"

organization := "io.prediction"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.prediction" %% "core" % "0.8.0-SNAPSHOT" % "provided",
  "org.scalanlp"   % "nak"  % "1.2.1")
