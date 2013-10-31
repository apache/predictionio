name := "predictionio-commons"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Commons API Documentation")

libraryDependencies ++= Seq(
  "com.twitter" %% "chill" % "0.2.3",
  "com.typesafe" % "config" % "1.0.2",
  "org.mongodb" %% "casbah" % "2.6.2")
