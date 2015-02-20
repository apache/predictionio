name := "e2"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion.value % "provided",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test")
