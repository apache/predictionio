package io.prediction.examples.java.parallel;

import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.PJavaPreparator;

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
