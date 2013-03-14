package io.prediction.commons.scalding.modeldata.mongodb.examples

import com.twitter.scalding._

import io.prediction.commons.scalding.modeldata.mongodb.{MongoItemRecScoresSource, MongoItemSimScoresSource}

//TODO: clean up this example. see if there is better way to test MongoSource?
class ReadWrite(args: Args) extends Job(args) {
  
  /**
   * test MongoItemSimScoresSource
   * read from Tsv and write to DB
   */
  val itemSimSink = new MongoItemSimScoresSource("modeldata", "127.0.0.1", 27017)
  
  Tsv("itemSimScores.tsv").read
    .mapTo((0, 1, 2, 3) -> ('iid, 'simiid, 'score, 'simitypes)) { fields: (String, String, Double, String) =>
      val (iid, simiid, score, simitypes) = fields
      
      (iid, simiid, score, simitypes.split(",").toList)
    }
    .then( itemSimSink.writeData('iid, 'simiid, 'score, 'simitypes, 12, false) _ )
  
  /**
   * test MongoItemRecScoresSource
   * read from Tsv and write to DB
   */
  val itemRecSink = new MongoItemRecScoresSource("modeldata", "127.0.0.1", 27017) 
    
  Tsv("itemRecScores.tsv").read
    .mapTo((0, 1, 2, 3) -> ('uid, 'iid, 'score, 'itypes)) { fields: (String, String, Double, String) =>
      val (uid, iid, score, itypes) = fields
      
      (uid, iid, score, itypes.split(",").toList)
    }
    .then( itemRecSink.writeData('uid, 'iid, 'score, 'itypes, 3, true) _  )
    
}
