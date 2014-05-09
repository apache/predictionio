name := "predictionio-output"

scalacOptions in (Compile, doc) ++= Opts.doc.title(
  "PredictionIO Output API Documentation")

libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "0.7")
