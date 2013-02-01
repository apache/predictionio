package com.clojurewerkz.cascading.mongodb;

import com.mongodb.BasicDBList;

//NOTE: this TsBasicDBList class is used to work around the issue that cascading
//expecting 'comparable' Tuple element but original BasicDBList doesn't implement
//comparable interface.

/* TODO: this doesn't really work. may get following exception if use this with groupBy operation.
 * probably need to implement correct deserialization function and register in hadoop in order to work.
 * see https://groups.google.com/forum/?fromgroups=#!searchin/cascading-user/primitive$20type$20array/cascading-user/ksHVOKngo8M/YZd3WVvwmOIJ
 *  
Caused by: java.lang.NullPointerException
	at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:186)
	at java.util.ArrayList.ensureCapacity(ArrayList.java:180)
	at com.esotericsoftware.kryo.serializers.CollectionSerializer.read(CollectionSerializer.java:91)
	at com.esotericsoftware.kryo.serializers.CollectionSerializer.read(CollectionSerializer.java:18)
	at com.esotericsoftware.kryo.Kryo.readObject(Kryo.java:615)
	at cascading.kryo.KryoDeserializer.deserialize(KryoDeserializer.java:37)
	at cascading.tuple.hadoop.TupleSerialization$SerializationElementReader.read(TupleSerialization.java:590)
	at cascading.tuple.hadoop.io.HadoopTupleInputStream.readType(HadoopTupleInputStream.java:105)
	at cascading.tuple.hadoop.io.HadoopTupleInputStream.getNextElement(HadoopTupleInputStream.java:52)
	at cascading.tuple.io.TupleInputStream.readTuple(TupleInputStream.java:78)
	at cascading.tuple.io.TupleInputStream.readTuple(TupleInputStream.java:67)
	at cascading.tuple.hadoop.io.TupleDeserializer.deserialize(TupleDeserializer.java:38)
	at cascading.tuple.hadoop.io.TupleDeserializer.deserialize(TupleDeserializer.java:28)
	at org.apache.hadoop.mapred.Task$ValuesIterator.readNextValue(Task.java:991)
	at org.apache.hadoop.mapred.Task$ValuesIterator.next(Task.java:931)
	at org.apache.hadoop.mapred.ReduceTask$ReduceValuesIterator.moveToNext(ReduceTask.java:241)
	at org.apache.hadoop.mapred.ReduceTask$ReduceValuesIterator.next(ReduceTask.java:237)
	at cascading.flow.hadoop.util.TimedIterator.next(TimedIterator.java:74)
*/
public class TsBasicDBList extends BasicDBList implements Comparable {
	
	public TsBasicDBList(BasicDBList obj) {
		super();
		
		this.putAll(obj);
	}
	
	public int compareTo(Object o) {
		return 0; // TODO: implement meaningful compare. dummy compareTo function for now
	}
	
}