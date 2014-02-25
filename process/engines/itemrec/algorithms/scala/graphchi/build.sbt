import AssemblyKeys._

assemblySettings

name := "predictionio-process-itemrec-algorithms-scala-grapchi"

libraryDependencies ++= Seq(
	"org.scalanlp" % "breeze_2.10" % "0.6.1",
	"org.clapper" % "grizzled-slf4j_2.10" % "1.0.1",
	"com.twitter" % "scalding-args_2.10" % "0.8.11")
