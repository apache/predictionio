name := "predictionio-commons"

scalacOptions in (Compile, doc) ++= Opts.doc.title("PredictionIO Commons API Documentation")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.0.2",
  "org.json4s" %% "json4s-native" % "3.2.7",
  "org.json4s" %% "json4s-ext" % "3.2.7",
  "org.mongodb" %% "casbah" % "2.6.2")
