package io.prediction.commons.modeldata.mongodb

import io.prediction.commons.modeldata.{ItemSimScore,ItemSimScores}
import com.mongodb.casbah.Imports._
/*
class MongoItemSimScores(db: MongoDB) extends ItemSimScores {
  private val emptyObj = MongoDBObject()

  /** Get an ItemSimScore by id */
  def get(id: Int): Option[ItemSimScore] = {}

  /** Insert an ItemSimScore */
  def insert(itemsimscore: ItemSimScore): Int = {
    val id = 1

    id
  }

  /** Delete an ItemSimScore by id */
  def delete(id: Int) {}
}*/