package com.clojurewerkz.cascading.mongodb;

import com.mongodb.DBObject;
import com.mongodb.hadoop.MongoOutput;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

/**
 *
 */
class CascadingMongoOutput implements MongoOutput {

  //private ObjectId key;
  private Object key;
  private BSONObject value;

  /**
   *
   * @param key
   * @param value
   */
  //public CascadingMongoOutput(ObjectId key, BSONObject value) {
  public CascadingMongoOutput(Object key, BSONObject value) {
	if (this.key == null) {
      this.key = key;
    }

    this.value = value;
  }

  /**
   *
   * @param dbObject
   */
  @Override
  public void appendAsKey(DBObject dbObject) {
    dbObject.put("_id", this.key);
  }

  /**
   *
   * @param dbObject
   */
  @Override
  public void appendAsValue(DBObject dbObject) {
    dbObject.putAll(this.value);
  }

}
