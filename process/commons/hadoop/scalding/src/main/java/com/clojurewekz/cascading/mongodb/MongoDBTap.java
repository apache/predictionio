package com.clojurewerkz.cascading.mongodb;

import java.io.IOException;

import cascading.tuple.TupleEntryCollector;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.OutputCollector;

import cascading.flow.FlowProcess;
import cascading.tap.Tap;
import cascading.tap.hadoop.io.HadoopTupleEntrySchemeIterator;
import cascading.tap.hadoop.io.RecordReaderIterator;
import cascading.tuple.TupleEntryIterator;
import cascading.tuple.TupleEntrySchemeIterator;

import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoConfigUtil;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MongoDBTap extends Tap<JobConf, RecordReader, OutputCollector> {

  public final String id = "TEMP_ID";

  public MongoDBScheme scheme;

  public MongoDBTap(MongoDBScheme scheme) {
    super(scheme);
    this.scheme = scheme;
  }

  //
  // Reader and writer initialization
  //

  /**
   *
   * @param flowProcess
   * @param reader
   * @return
   * @throws IOException
   */
  @Override
  public TupleEntryIterator openForRead(FlowProcess<JobConf> flowProcess, RecordReader reader) throws IOException {
    return new HadoopTupleEntrySchemeIterator(flowProcess, this , reader);
  }

  /**
   *
   * @param flowProcess
   * @param outputCollector
   * @return
   * @throws IOException
   */
  @Override
  public TupleEntryCollector openForWrite(FlowProcess<JobConf> flowProcess, OutputCollector outputCollector) throws IOException {
    MongoDbCollector collector = new MongoDbCollector(flowProcess, this);
    collector.prepare();
    return collector;
  }

  //
  // Resource manipulation
  //

  /**
   *
   * @param jobConf
   * @return
   * @throws IOException
   */
  @Override
  public boolean createResource(JobConf jobConf) throws IOException {
    // TODO
    return true;
  }


  /**
   *
   * @param jobConf
   * @return
   * @throws IOException
   */
  @Override
  public boolean deleteResource(JobConf jobConf) throws IOException {
    // TODO
    return true;
  }

  /**
   *
   * @param jobConf
   * @return
   * @throws IOException
   */
  @Override
  public boolean resourceExists(JobConf jobConf) throws IOException {
    // TODO check if column-family exists
    return true;
  }

  //
  // Helper methods
  //

  /**
   *
   * @param jobConf
   * @return
   * @throws IOException
   */
  @Override
  public long getModifiedTime(JobConf jobConf) throws IOException {
    // TODO could read this from tables
    return System.currentTimeMillis();
  }

  /**
   *
   * @param other
   * @return
   */
  @Override
  public boolean equals(Object other) {
    if( this == other )
      return true;
    if( !( other instanceof MongoDBTap ) )
      return false;
    if( !super.equals( other ) )
      return false;

    MongoDBTap otherTap = (MongoDBTap) other;
    if (!otherTap.getIdentifier().equals(getIdentifier())) return false;

    return true;
  }

  /**
   *
   * @return
   */
  @Override
  public int hashCode(){
    int result = super.hashCode();
    result = 31 * result + getIdentifier().hashCode();

    return result;
  }

  /**
   *
   * @return
   */
  @Override
  public String getIdentifier() {
    return id + "_" + scheme.getIdentifier();
  }

}
