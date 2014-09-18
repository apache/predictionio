package io.prediction.engines.java.olditemrec.data

//import java.io.Serializable
import java.util.{ List => JList }
import java.lang.{ Integer => JInteger }
import java.lang.{ Float => JFloat }

class Prediction(val iids: JList[JInteger], val scores: JList[JFloat])
    extends Serializable {
  override def toString(): String =
    s"Prediction: ${iids.toString()} ; ${scores.toString()}"
}


/*
public class Prediction implements Serializable {
  public List<Integer> iids;
  public List<Float> scores;

  public Prediction(List<Integer> iids, List<Float> scores) {
    this.iids = iids;
    this.scores = scores;
  }

  @Override
  public String toString() {
    return "Prediction: " + iids.toString() + ";" + scores.toString();
  }
}
*/
