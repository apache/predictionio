package io.prediction.commons.filepath

/**
 * These objects are used for returning HDFS file path
 */

object BaseDir {
  
  def appDir(appId: Int): String = "apps/" + appId + "/"
  
  def engineDir(appId: Int, engineId: Int): String = appDir(appId) + "engines/" + engineId + "/"
  
  def offlineEvalDir(appId: Int, engineId: Int, evalId: Int): String = engineDir(appId, engineId) + "offlineeval/" + evalId + "/"

  def algoDir(appId: Int, engineId: Int, algoId: Int, evalId: Option[Int]): String = {
    evalId match {
      case Some(eva) => offlineEvalDir(appId, engineId, eva) + "algos/" + algoId + "/"
      case _ => engineDir(appId, engineId) + "batch/" + "algos/" + algoId + "/"
    }
  }

  def offlineMetricDir(appId: Int, engineId: Int, algoId: Int, evalId: Int, metricId: Int): String = {
    offlineEvalDir(appId, engineId, evalId) + "metrics/" + metricId + "/algos/" + algoId + "/"
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
    rootDir + BaseDir.algoDir(appId, engineId, algoId, evalId) + "data/" + name
    
}

/**
 * For Data Preparator output
 */
object AlgoFile {
  
  def apply(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int], name: String): String =
    rootDir + BaseDir.algoDir(appId, engineId, algoId, evalId) + "algo/" + name
    
}

/**
 * Metrics Data Preparator output file
 */
object OfflineMetricFile {
  
  def apply(rootDir: String, appId: Int, engineId: Int, evalId: Int, metricId: Int, algoId: Int, name: String): String =
    rootDir + BaseDir.offlineMetricDir(appId, engineId, algoId, evalId, metricId) + "metric/" + name
}


/*
 * standard
 */

/**
 * For Model Constructor output
 */
object ModelDataDir {
  
  def apply(rootDir: String, appId: Int, engineId: Int, algoId: Int, evalId: Option[Int]): String =
    rootDir + BaseDir.algoDir(appId, engineId, algoId, evalId) + "modeldata/"
}

/**
 * Offline Evaluation output file
 */
object OfflineEvalResultsDir {
  
  def apply(rootDir: String, appId: Int, engineId: Int, evalId: Int, metricId: Int, algoId: Int): String =
    rootDir + BaseDir.offlineMetricDir(appId, engineId, algoId, evalId, metricId) + "evalresults/"
}

/**
 * AppData
 */
object AppDataDir {
  
  def apply(rootDir: String, appId: Int, engineId: Option[Int], evalId: Option[Int], testSet: Option[Boolean]): String = {
    (engineId, evalId, testSet) match {
      case (Some(eng), Some(eva), Some(test)) => rootDir + BaseDir.offlineEvalDir(appId, eng, eva) + "appdata/" + (if (test) "test/" else "training/")
      case _ => rootDir + BaseDir.appDir(appId) + "appdata/"
    }
  }
}


