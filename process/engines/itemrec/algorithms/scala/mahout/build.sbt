import xerial.sbt.Pack._

name := "predictionio-process-itemrec-algorithms-scala-mahout"

parallelExecution in Test := false

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemrec.mahout.mahoutjob" -> "io.prediction.commons.mahout.itemrec.MahoutJob")

packJvmOpts := Map(
  "itemrec.mahout.mahoutjob" -> Common.packCommonJvmOpts)
