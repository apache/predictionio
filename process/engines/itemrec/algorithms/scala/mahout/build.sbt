import xerial.sbt.Pack._

name := "predictionio-process-itemrec-algorithms-scala-mahout"

parallelExecution in Test := false

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemrec.mahout.mahoutjob" -> "io.prediction.algorithms.mahout.itemrec.MahoutJob",
  "itemrec.mahout.modelcon" -> "io.prediction.algorithms.mahout.itemrec.MahoutModelConstructor")

packJvmOpts := Map(
  "itemrec.mahout.mahoutjob" -> Common.packCommonJvmOpts,
  "itemrec.mahout.modelcon" -> Common.packCommonJvmOpts)
