package io.prediction.commons.filepath

/**
 * These objects are used for returning HDFS file path
 */

object BaseDir {
  
  def appDir(rootDir: String, appId: Int): String = rootDir + "apps/" + appId + "/"
  
  def engineDir(rootDir: String, appId: Int, engineId: Int): String = appDir(rootDir, appId) + "engines/" + engineId + "/"
  
  def offlineEvalDir(rootDir: String, appId: Int, engineId: Int, evalId: Int): String = engineDir(rootDir, appId, engineId) + "offlineeval/" + evalId + "/"

  def algoDir(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int]): String = {
    evalId match {
      case Some(eva) => offlineEvalDir(rootDir, appId, engineId, eva) + "algos/" + algoId + "/"
      case _ => engineDir(rootDir, appId, engineId) + "batch/" + "algos/" + algoId + "/"
    }
  }

  def offlineMetricDir(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Int, metricId: Int): String = {
    offlineEvalDir(rootDir, appId, engineId, evalId) + "metrics/" + metricId + "/algos/" + algoId + "/"
  }
  
}

/*
 * process internal file 
 */

/**
 * For Data Preparator output
 */
object DataFile {
  
  def apply(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int], name: String): String =
    BaseDir.algoDir(rootDir, appId, engineId, algoId, evalId) + "data/" + name
    
}

/**
 * For Data Preparator output
 */
object AlgoFile {
  
  def apply(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int], name: String): String =
    BaseDir.algoDir(rootDir, appId, engineId, algoId, evalId) + "algo/" + name
    
}

/**
 * Metrics Data Preparator output file
 */
object OfflineMetricFile {
  
  def apply(rootDir: String, appId: Int, engineId: Int, evalId: Int, metricId: Int, algoId: Int, name: String): String =
    BaseDir.offlineMetricDir(rootDir, appId, engineId, algoId, evalId, metricId) + "metric/" + name
}

/**
 * Training Test Set Generator Internal File
 */
object U2ITrainingTestSplitFile {

  def apply(rootDir: String, appId: Int, engineId: Int, evalId: Int, name: String): String =
    BaseDir.offlineEvalDir(rootDir, appId, engineId, evalId) + "u2itrainingtestsplit/" + name
}

/*
 * standard
 */

/**
 * For Model Constructor output
 */
object ModelDataDir {
  
  def apply(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int]): String =
    BaseDir.algoDir(rootDir, appId, engineId, algoId, evalId) + "modeldata/"
}

/**
 * Offline Evaluation output file
 */
object OfflineEvalResultsDir {
  
  def apply(rootDir: String, appId: Int, engineId: Int, evalId: Int, metricId: Int, algoId: Int): String =
    BaseDir.offlineMetricDir(rootDir, appId, engineId, algoId, evalId, metricId) + "evalresults/"
}

/**
 * AppData
 */
object AppDataDir {
  
  def apply(rootDir: String, appId: Int, engineId: Option[Int], evalId: Option[Int], testSet: Option[Boolean]): String = {
    (engineId, evalId, testSet) match {
      case (Some(eng), Some(eva), Some(test)) => BaseDir.offlineEvalDir(rootDir, appId, eng, eva) + "appdata/" + (if (test) "test/" else "training/")
      case _ => BaseDir.appDir(rootDir, appId) + "appdata/"
    }
  }
}


