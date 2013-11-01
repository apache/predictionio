package io.prediction.commons.filepath

import org.specs2.mutable._

class EngineFileTest extends Specification {
  
  "BaseDir" should {
    "correctly return appDir" in {
      BaseDir.appDir("testroot/", appId=4) must be_==("testroot/apps/4/")
    }
    "correctly return engineDir" in {
      BaseDir.engineDir("testroot/", appId=4, engineId=5) must be_==("testroot/apps/4/engines/5/")
    }
    "correctly return offlineEvalDir" in {
      BaseDir.offlineEvalDir("testroot/", appId=4, engineId=5, evalId=6) must be_==("testroot/apps/4/engines/5/offlineeval/6/")
    }
    "correctly return algoDir" in {
      BaseDir.algoDir("testroot/", appId=4, engineId=5, algoId=7, evalId=None) must be_==("testroot/apps/4/engines/5/batch/algos/7/")
    }
    "correctly return algoDir for offline eval" in {
      BaseDir.algoDir("testroot/", appId=4, engineId=5, algoId=7, evalId=Some(8)) must be_==("testroot/apps/4/engines/5/offlineeval/8/algos/7/")
    }
    "correctly return offlineMetricDir" in {
      BaseDir.offlineMetricDir("testroot/", appId=4, engineId=5, algoId=17, evalId=8, metricId=9) must be_==("testroot/apps/4/engines/5/offlineeval/8/metrics/9/algos/17/")
    }
  }
  
  // simple tests
  "DataFile" should {
    "correctly return path in batch mode" in {
      DataFile("hdfs/predictionio/", 21, 4, 20, None, "test6.tsv") must be_==("hdfs/predictionio/apps/21/engines/4/batch/algos/20/data/test6.tsv")
    }
    "correctly return path in offline eval mode" in {
      DataFile("hdfs/predictionio/", 12, 5, 1, Some(9), "test7.tsv") must be_==("hdfs/predictionio/apps/12/engines/5/offlineeval/9/algos/1/data/test7.tsv")
    }
  }
    
  "AlgoFile" should {
    "correctly return path in batch mode" in {
      AlgoFile("hdfs/predictionio/", 22, 4, 20, None, "test10.tsv") must be_==("hdfs/predictionio/apps/22/engines/4/batch/algos/20/algo/test10.tsv")
    }
    "correctly return path in offline eval mode" in {
      AlgoFile("hdfs/predictionio/", 23, 3, 12, Some(101), "test22.tsv") must be_==("hdfs/predictionio/apps/23/engines/3/offlineeval/101/algos/12/algo/test22.tsv")
    }
  }
    
  "OfflineMetricFile" should {
    "corretly return path" in {
      OfflineMetricFile("hdfs/predictionio/",2,11,22,33,44,"test.tsv") must be_==("hdfs/predictionio/apps/2/engines/11/offlineeval/22/metrics/33/algos/44/metric/test.tsv")
    }
  }

  "U2ITrainingTestSplitFile" should {
    "correctly return path" in {
      U2ITrainingTestSplitFile("hdfs/predictionio/", 2, 4, 6, "test8.tsv") must be_==("hdfs/predictionio/apps/2/engines/4/offlineeval/6/u2itrainingtestsplit/test8.tsv")
    }
  }
  
  "ModelDataDir" should {
    "correctly return path in batch mode" in {
      ModelDataDir("hdfs/predictionio/", 24, 6, 7, None) must be_==("hdfs/predictionio/apps/24/engines/6/batch/algos/7/modeldata/")
    }
    "correctly return path in offline eval mode" in {
      ModelDataDir("hdfs/predictionio/", 21, 5, 6, Some(11)) must be_==("hdfs/predictionio/apps/21/engines/5/offlineeval/11/algos/6/modeldata/")
    }
  }
    
  "OfflineEvalResultsDir" should {
    "corretly return path" in {
      OfflineEvalResultsDir("hdfs/predictionio/",3,12,23,34,45) must be_==("hdfs/predictionio/apps/3/engines/12/offlineeval/23/metrics/34/algos/45/evalresults/")
    }
  }
  
  "AppDataDir" should {
    "correctly return path" in {
      AppDataDir("hdfs/predictionio/", 8, None, None, None) must be_==("hdfs/predictionio/apps/8/appdata/")
    }
    "correctly return path in offline eval mode and test set" in {
      AppDataDir("hdfs/predictionio/", 7, Some(2), Some(3), Some(true)) must be_==("hdfs/predictionio/apps/7/engines/2/offlineeval/3/appdata/test/")
    }
    "correctly return path in offline eval mode and training set" in {
      AppDataDir("hdfs/predictionio/", 6, Some(2), Some(3), Some(false)) must be_==("hdfs/predictionio/apps/6/engines/2/offlineeval/3/appdata/training/")
    }
    
  }
    
}
