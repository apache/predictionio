name := "predictionio-output"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Output API Documentation")

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.14" % "test")
