package io.prediction.commons.scalding.modeldata.mongodb.examples

import com.twitter.scalding._

import io.prediction.commons.scalding.modeldata.mongodb.{ MongoItemRecScoresSource, MongoItemSimScoresSource }

//TODO: clean up this example. see if there is better way to test MongoSource?
class ReadWrite(args: Args) extends Job(args) {

  /**
   * test MongoItemSimScoresSource
   * read from Tsv and write to DB
   */
  val itemSimSink = new MongoItemSimScoresSource("test", Seq("localhost"), Seq(27017), 2, true)

  Tsv("itemSimScores.tsv").read
    .mapTo((0, 1, 2, 3) -> ('iid, 'simiid, 'score, 'simitypes)) { fields: (String, String, Double, String) =>
      val (iid, simiid, score, simitypes) = fields

      (iid, simiid, score, simitypes.split(",").toList)
    }
    .groupBy('iid) { _.sortBy('score).reverse.toList[(String, Double, List[String])](('simiid, 'score, 'simitypes) -> 'simiidsList) }
    .then(itemSimSink.writeData('iid, 'simiidsList, 12, false) _)

  /**
   * test MongoItemRecScoresSource
   * read from Tsv and write to DB
   */
  val itemRecSink = new MongoItemRecScoresSource("test", Seq("localhost"), Seq(27017), 1, true)

  Tsv("itemRecScores.tsv").read
    .mapTo((0, 1, 2, 3) -> ('uid, 'iid, 'score, 'itypes)) { fields: (String, String, Double, String) =>
      val (uid, iid, score, itypes) = fields

      (uid, iid, score, itypes.split(",").toList)
    }
    .groupBy('uid) { _.sortBy('score).reverse.toList[(String, Double, List[String])](('iid, 'score, 'itypes) -> 'iidsList) }
    .then(itemRecSink.writeData('uid, 'iidsList, 3, true) _)

}
