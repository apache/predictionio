package com.clojurewerkz.cascading.mongodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cascading.tuple.FieldsResolverException;
import cascading.tuple.TupleEntry;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.mapred.MongoOutputFormat;
import com.mongodb.DBObject;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.mapred.MongoInputFormat;
import com.mongodb.hadoop.util.MongoConfigUtil;

@SuppressWarnings("rawtypes")
public class MongoDBScheme extends Scheme<JobConf, RecordReader, OutputCollector, BSONWritable[], BSONWritable[]> {

  /**
   * Field logger
   */
  private static final Logger logger = LoggerFactory.getLogger(MongoDbCollector.class);

  private String pathUUID;
  public String mongoUri;
  public List<String> columnFieldNames;
  public Map<String, String> fieldMappings;
  public String keyColumnName;

  private String host;
  private Integer port;
  private String database;
  private String collection;
  private DBObject query;

  // with default id, without query
  public MongoDBScheme(String host, Integer port, String database, String collection, List<String> columnFieldNames, Map<String, String> fieldMappings) {
    this(host, port, database, collection, "_id", columnFieldNames, fieldMappings);
  }
  
  // with default id and query
  public MongoDBScheme(String host, Integer port, String database, String collection, List<String> columnFieldNames, Map<String, String> fieldMappings, DBObject query) {
	    this(host, port, database, collection, "_id", columnFieldNames, fieldMappings, query);
  }

  // without query
  public MongoDBScheme(String host, Integer port, String database, String collection, String keyColumnName, List<String> columnFieldNames, Map<String, String> fieldMappings) {
	  this(host, port, database, collection, keyColumnName, columnFieldNames, fieldMappings, new BasicDBObject());
  }
		  
  public MongoDBScheme(String host, Integer port, String database, String collection, String keyColumnName, List<String> columnFieldNames, Map<String, String> fieldMappings, DBObject query) {
    this.mongoUri = String.format("mongodb://%s:%d/%s.%s", host, port, database, collection);
    this.pathUUID = UUID.randomUUID().toString();
    this.columnFieldNames = columnFieldNames;
    this.fieldMappings = fieldMappings;
    this.keyColumnName = keyColumnName;

    this.host = host;
    this.port = port;
    this.database = database;
    this.collection = collection;
    this.query = query;
  }

  /**
   *
   * @return
   */
  public Path getPath() {
    return new Path(pathUUID);
  }

  /**
   *
   * @return
   */
  public String getIdentifier() {
    return String.format("%s_%d_%s_%s", this.host, this.port, this.database, this.collection);
  }

  /**
   *
   * @param process
   * @param tap
   * @param conf
   */
  @Override
  public void sourceConfInit(FlowProcess<JobConf> process, Tap<JobConf, RecordReader, OutputCollector> tap, JobConf conf) {
    MongoConfigUtil.setInputURI( conf, this.mongoUri );
    FileInputFormat.setInputPaths(conf, this.getIdentifier());
    conf.setInputFormat(MongoInputFormat.class);

    // TODO: MongoConfigUtil.setFields(conf, fieldsBson);
    //if (!this.query.isEmpty())
    MongoConfigUtil.setQuery(conf, this.query);
    // TODO: MongoConfigUtil.setFields(conf, fields);
  }

  /**
   *
   * @param process
   * @param tap
   * @param conf
   */
  @Override
  public void sinkConfInit(FlowProcess<JobConf> process, Tap<JobConf, RecordReader, OutputCollector> tap,
                           JobConf conf) {
    conf.setOutputFormat(MongoOutputFormat.class);
    MongoConfigUtil.setOutputURI(conf, this.mongoUri);

    //FileOutputFormat.setOutputPath(conf, getPath());
  }

  /**
   *
   * @param flowProcess
   * @param sourceCall
   */
  @Override
  public void sourcePrepare(FlowProcess<JobConf> flowProcess, SourceCall<BSONWritable[], RecordReader> sourceCall) {
    sourceCall.setContext(new BSONWritable[2]);

    sourceCall.getContext()[0] = (BSONWritable) sourceCall.getInput().createKey();
    sourceCall.getContext()[1] = (BSONWritable) sourceCall.getInput().createValue();
  }

  /**
   *
   * @param flowProcess
   * @param sourceCall
   * @return
   * @throws IOException
   */
  @Override
  public boolean source(FlowProcess<JobConf> flowProcess, SourceCall<BSONWritable[], RecordReader> sourceCall) throws IOException {
    Tuple result = new Tuple();

    BSONWritable key = sourceCall.getContext()[0];
    BSONWritable value = sourceCall.getContext()[1];

    if (!sourceCall.getInput().next(key, value)) {
      logger.info("Nothing left to read, exiting");
      return false;
    }

    for (String columnFieldName : columnFieldNames) {
      Object tupleEntry= value.getDoc().get(columnFieldName);
      if (tupleEntry != null) {
        result.add(tupleEntry);
      } else if (columnFieldName != this.keyColumnName) {
        result.add(null);
      }
    }

    sourceCall.getIncomingEntry().setTuple(result);
    return true;
  }

  /**
   *
   * @param flowProcess
   * @param sinkCall
   * @throws IOException
   */
  @Override
  public void sink(FlowProcess<JobConf> flowProcess, SinkCall<BSONWritable[], OutputCollector> sinkCall) throws IOException {
    TupleEntry tupleEntry = sinkCall.getOutgoingEntry();
    OutputCollector outputCollector = sinkCall.getOutput();

    String keyFieldName = this.fieldMappings.get(this.keyColumnName);
    Object key;
    
    // if fieldMappings doesn't have keyColumnName ("_id") field, then use new ObjectId() as key
    if (keyFieldName == null) {
	key = null;
    } else {
	key = new Text((String)(tupleEntry.selectTuple(new Fields(keyFieldName)).get(0)));
    }
    //Object key = tupleEntry.selectTuple(new Fields(this.fieldMappings.get(this.keyColumnName))).get(0);

    BasicDBObject dbObject = new BasicDBObject();

    for (String columnFieldName : columnFieldNames) {
      String columnFieldMapping = fieldMappings.get(columnFieldName);
      Object tupleEntryValue = null;
      
      try {
        if (columnFieldMapping != null) {
          // columnFieldMapping is null if no corresponding field name defined in Mappings.
          // only write the field value back to mongo if the field also defined in Mappings (ie. not null)
          tupleEntryValue = tupleEntry.get(columnFieldMapping);
        }
      } catch(FieldsResolverException e) {
        logger.error("Couldn't resolve field: {}", columnFieldName);
      }

      if(tupleEntryValue != null && columnFieldName != keyColumnName) {
        //logger.info("Putting for output: {} {}", columnFieldName, tupleEntryValue);
        dbObject.put(columnFieldName, tupleEntryValue);
      }
    }
    //logger.info("Putting key for output: {} {}", key, dbObject);
    outputCollector.collect(key, new BSONWritable(dbObject));
  }

}
