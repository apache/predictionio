package io.prediction.storage

import com.mongodb.casbah.Imports._

object MongoEngineManifests {
  //RegisterJodaTimeConversionHelpers()

  def apply(db: MongoDB): MongoEngineManifests = new MongoEngineManifests(db)
}

/** MongoDB implementation of EngineManifests. */
class MongoEngineManifests(db: MongoDB) extends EngineManifests {
  private val coll = db("engineManifests")

  private def dbObjToEngineManifest(dbObj: DBObject) = EngineManifest(
    id = dbObj.as[String]("_id"),
    version = dbObj.as[String]("version"),
    name = dbObj.as[String]("name"),
    description = dbObj.getAs[String]("description"),
    jars = dbObj.as[Seq[String]]("jars"),
    engineFactory = dbObj.as[String]("engineFactory"),
    evaluatorFactory = dbObj.as[String]("evaluatorFactory"))

  def insert(engineManifest: EngineManifest) = {
    // required fields
    val obj = MongoDBObject(
      "_id" -> engineManifest.id,
      "version" -> engineManifest.version,
      "name" -> engineManifest.name,
      "jars" -> engineManifest.jars,
      "engineFactory" -> engineManifest.engineFactory,
      "evaluatorFactory" -> engineManifest.evaluatorFactory)

    // optional fields
    val optObj = engineManifest.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ optObj)
  }

  def get(id: String, version: String) = coll.findOne(MongoDBObject("_id" -> id, "version" -> version)) map { dbObjToEngineManifest(_) }

  def getAll() = coll.find().toSeq map { dbObjToEngineManifest(_) }

  def update(engineManifest: EngineManifest, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> engineManifest.id)
    val requiredObj = MongoDBObject(
      "version" -> engineManifest.version,
      "name" -> engineManifest.name,
      "jars" -> engineManifest.jars,
      "engineFactory" -> engineManifest.engineFactory,
      "evaluatorFactory" -> engineManifest.evaluatorFactory)
    val descriptionObj = engineManifest.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj, upsert)
  }

  def delete(id: String, version: String) = coll.remove(MongoDBObject("_id" -> id, "version" -> version))
}
