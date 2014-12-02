---
title: Building the Java Parallel "HelloWorld" Engine
---

# Building the Java Parallel "HelloWorld" Engine

This is similar to the [HelloWorld](local-helloworld.html) engine tutorial.
The engine will use historical temperature data to predict the temperature of
a certain day in a week. We assume you have gone through tutorial for the
local version.

> You need to build PredictionIO from source in order to build your own engine.
Please follow instructions to build from source
[here](/install/install-sourcecode.html).

Completed source code can also be found at
`$PIO_HOME/examples/java-parallel-helloworld`, where `$PIO_HOME` is the root
directory of the PredictionIO source code tree.

## 1. Define Data Types

### Define Training Data

Training data in this case is of type `JavaPairRDD<String, Float>` where
`String` holds the day and `Float` holds the temperature:

<div class="tabs">
<div data-tab="Java" data-lang="java">
```java
import org.apache.spark.api.java.JavaPairRDD;

JavaPairRDD<String, Float> readings;
```
  </div>
</div>

### Define Prepared Data

We convert the temperatures from degrees Fahrenheit to degrees Celsius, so
Prepared Data also has type `JavaPairRDD<String, Float>`

### Define Query

This is the same as the local counterpart.

<div class="tabs">
  <div data-tab="Java" data-lang="java">
```java
public class Query implements Serializable {
  String day;

  public Query(String day) {
    this.day = day;
  }
}
```
  </div>
</div>

### Define Model

Our Model is of the same type as the Training Data, i.e.
`JavaPairRDD<String, Float>`. To have it output the contents instead of an
address when we do `toString()`, we override it:

<div class="tabs">
  <div data-tab="Java" data-lang="java">
```java
public JavaPairRDD<String, Float> temperatures;

@Override
public String toString() {
  boolean longList = temperatures.count() > LIST_THRESHOLD ? true : false;
  List<Tuple2<String, Float>> readings =
    temperatures.take(longList ? LIST_THRESHOLD : (int) temperatures.count());
  StringBuilder builder = new StringBuilder();
  builder.append("(");
  boolean first = true;
  for (Tuple2<String, Float> reading : readings) {
    if (!first) {
      builder.append(", ");
    } else {
      first = false;
    }
    builder.append(reading);
  }
  if (longList) {
    builder.append(", ...");
  }
  builder.append(")");
  return builder.toString();
}
```
  </div>
</div>

### Define Prediction Result

Prediction result is simply a `Float`.

## 2. Implement the Data Source

Only scala and spark related imports are shown; refer to the source code for
the whole thing.

<div class="tabs">
  <div data-tab="Java" data-lang="java">
```java
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple3;

public class DataSource extends PJavaDataSource<
  EmptyParams, Object, JavaPairRDD<String, Float>, Query, Object> {
  @Override
  public Iterable<Tuple3<Object, JavaPairRDD<String, Float>, JavaPairRDD<Query, Object>>>
      read(JavaSparkContext jsc) {
    JavaPairRDD<String, Float> readings = jsc.textFile("path/to/data.csv")
      .mapToPair(new PairFunction<String, String, Float>() {
        @Override
        public Tuple2 call(String line) {
          String[] tokens = line.split("[\t,]");
          Tuple2 reading = null;
          try {
            reading = new Tuple2(
              tokens[0],
              Float.parseFloat(tokens[1]));
          } catch (Exception e) {
            logger.error("Can't parse reading file. Caught Exception: " + e.getMessage());
            System.exit(1);
          }
          return reading;
        }
      });

    List<Tuple3<Object, JavaPairRDD<String, Float>, JavaPairRDD<Query, Object>>> data =
      new ArrayList<>();

    data.add(new Tuple3(
      null,
      readings,
      jsc.parallelizePairs(new ArrayList<Tuple2<Query, Object>>())
    ));

    return data;
  }
}
```
  </div>
</div>

## 3. Implement the Preparator

As mentioned above, we convert the scale from Fahrenheit to Celsius:

<div class="tabs">
  <div data-tab="Java" data-lang="java">
```java
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

public class Preparator extends
  PJavaPreparator<EmptyParams, JavaPairRDD<String, Float>, JavaPairRDD<String, Float>> {

  @Override
  public JavaPairRDD<String, Float> prepare(JavaSparkContext jsc,
      JavaPairRDD<String, Float> data) {
    return data.mapValues(new Function<Float, Float>() {
        @Override
        public Float call(Float temperature) {
          // let's convert it to degrees Celsius
          return (temperature - 32.0f) / 9 * 5;
        }
      });
  }
}
```
  </div>
</div>

## 4. Implement an Algorithm

We need to implement `train()`, `batchPredict()` and `predict()`. We create
a `ReadingAndCount` class for sake of doing aggregation (taking average).

<div class="tabs">
<div data-tab="Java" data-lang="java">
```java
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class Algorithm extends PJavaAlgorithm<
  EmptyParams, JavaPairRDD<String, Float>, Model, Query, Float> {

  final static Logger logger = LoggerFactory.getLogger(Algorithm.class);

  public static class ReadingAndCount implements Serializable {
    public float reading;
    public int count;

    public ReadingAndCount(float reading, int count) {
      this.reading = reading;
      this.count = count;
    }

    public ReadingAndCount(float reading) {
      this(reading, 1);
    }

    @Override
    public String toString() {
      return "(reading = " + reading + ", count = " + count + ")";
    }
  }

  @Override
  public Model train(JavaPairRDD<String, Float> data) {
    // take averages just like the local helloworld program
    JavaPairRDD<String, Float> averages = data.mapValues(
      new Function<Float, ReadingAndCount>() {
        @Override
        public ReadingAndCount call(Float reading) {
          return new ReadingAndCount(reading);
        }
      }).reduceByKey(
      new Function2<ReadingAndCount, ReadingAndCount, ReadingAndCount>() {
        @Override
        public ReadingAndCount call(ReadingAndCount rac1, ReadingAndCount rac2) {
          return new ReadingAndCount(rac1.reading + rac2.reading, rac1.count + rac2.count);
        }
      }).mapValues(
      new Function<ReadingAndCount, Float>() {
        @Override
        public Float call(ReadingAndCount rac) {
          return rac.reading / rac.count;
        }
      });
    return new Model(averages);
  }

  @Override
  public JavaPairRDD<Object, Float> batchPredict(Model model,
      JavaPairRDD<Object, Query> indexedQueries) {
    return model.temperatures.join(indexedQueries.mapToPair(
        new PairFunction<Tuple2<Object, Query>, String, Object>() {
          @Override   // reverse the query tuples, then join
          public Tuple2 call(Tuple2<Object, Query> tuple) {
            return new Tuple2(tuple._2.day, tuple._1);
          }
        })).mapToPair(
        new PairFunction<Tuple2<String, Tuple2<Float, Object>>, Object, Float>() {
          @Override   // map result back to predictions, dropping the day
          public Tuple2 call(Tuple2<String, Tuple2<Float, Object>> tuple) {
            return new Tuple2(tuple._2._2, tuple._2._1);
          }
        });
  }

  @Override
  public Float predict(Model model, Query query) {
    final String day = query.day;
    List<Float> reading = model.temperatures.lookup(day);
    return reading.get(0);
  }
}
```
  </div>
</div>

## 5. Implement the Serving

Since there is only one algorithm, there is one prediction, so we just extract
it out. Note that we are using LJavaServing even though other stages are done
in a parallel manner.

<div class="tabs">
  <div data-tab="Java" data-lang="java">
```java
public class Serving extends LJavaServing<EmptyParams, Query, Float> {
  @Override
  public Float serve(Query query, Iterable<Float> predictions) {
    return predictions.iterator().next();
  }
}
```
  </div>
</div>

# Deploying the "HelloWorld" Engine Instance

After the new engine is built, it is time to deploy an engine instance of it.

Prepare training data:

```bash
$ cp $PIO_HOME/examples/data/helloworld/data1.csv path/to/data.csv
```

Register engine:

```bash
$ $PIO_HOME/bin/pio register
```

Train:

```bash
$ $PIO_HOME/bin/pio train
```

Example output:

```
2014-10-06 16:43:01,820 INFO  spark.SparkContext - Job finished: count at Workflow.scala:527, took 0.016301 s
2014-10-06 16:43:01,820 INFO  workflow.CoreWorkflow$ - DP 0 has 0 rows
2014-10-06 16:43:01,821 INFO  workflow.CoreWorkflow$ - Metrics is null. Stop here
2014-10-06 16:43:01,933 INFO  workflow.CoreWorkflow$ - Saved engine instance with ID: KzBHWQTsR9afg3_2mb5GfQ
```

Deploy:

```bash
$ $PIO_HOME/bin/pio deploy
```
Retrieve prediction:

```bash
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000/queries.json
```
Output:

```json
24.166668
```

Retrieve prediction:

```bash
$ curl -H "Content-Type: application/json" -d '{ "day": "Tue" }' http://localhost:8000/queries.json
```

Output:

```json
26.944447
```

## Re-training

Re-train with new data:

```bash
$ cp $PIO_HOME/examples/data/helloworld/data2.csv path/to/data.csv
```

```bash
$ $PIO_HOME/bin/pio train
$ $PIO_HOME/bin/pio deploy
```

Retrieve prediction:

```bash
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000/queries.json
```

Output:

```json
24.814814
```
