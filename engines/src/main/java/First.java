package io.prediction;

import io.prediction.engines.stock.TrainingData;

import org.saddle.Frame;
import org.joda.time.DateTime;
import scala.Tuple2;
import scala.Int;
//import scala.Double;
import java.util.HashMap;
import java.util.Map;





public class First {
  private int i = 0;

  public void inc(int k) {
    i += k;
  }

  public int get() {
    return i;
  }

  public void train(TrainingData trainingData) {
    System.out.println(trainingData);
    //System.out.println(trainingData.tickers);
    System.out.println(trainingData.mktTicker());
    //System.out.println(trainingData.tickers);

    System.out.println(trainingData.price());

    Frame<DateTime, String, Object> p = trainingData.price();



    System.out.println(p);
  }

  public void t(Tuple2<Int, Int> t) {
    System.out.println("First.f");
    System.out.println(t);
    System.out.println(t._1());
    System.out.println(t._2());


    /*
    //Tuple2<Int, Int> tt = new Tuple2<Int,Int>(Int(30), Int(40));
    Tuple2<Int, Int> tt = new Tuple2<Int,Int>(
        //scala.Int.unbox(new java.lang.Integer(3)),
        (new java.lang.Integer(3)).intValue(),
        scala.Int.unbox(new java.lang.Integer(20)));
    //t._1(30);
    System.out.println("New tt");
    System.out.println(tt);
    System.out.println(tt._1());
    System.out.println(tt._2());
    */
  }

  public void s(Integer i) {
    System.out.println(i);
  }

  /*
  public Int q() {
    //return 10;
    return scala.Int.unbox(new Integer(10));
  }
  */

  public Integer qq() {
    return new Integer(20);
  }

  public Map<Integer, Integer> p() {
    HashMap<Integer, Integer> m = new HashMap<Integer, Integer>();
    m.put(10, 300);
    m.put(20, 400);
    return m;
  }

  public static void main(String[] args) {
    System.out.println("Hello world!");
  }
}
