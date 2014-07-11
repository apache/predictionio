package io.prediction.engines.java.regression;
import io.prediction.java.JavaSimpleLocalDataPreparator;
import scala.Tuple2;

import java.util.List;
import java.lang.Iterable;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

public class DataPreparator
  extends JavaSimpleLocalDataPreparator<
    DataParams, TrainingData, Double[], Double> {
  private static final Pattern SPACE = Pattern.compile(" ");

  public Tuple2<TrainingData, Iterable<Tuple2<Double[], Double>>> 
    prepare(DataParams edp) {
 
    List<String> lines = new ArrayList<String>();
    try {
      lines = Files.readAllLines(Paths.get(edp.filepath), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      // FIXME. Should not silent catched exceptions...
      System.out.println("exception");
    }

    int n = lines.size();

    int featureCount = SPACE.split(lines.get(0)).length - 1;

    Double[][] x = new Double[n][featureCount]; 
    Double[] y = new Double[n];

    for (int i = 0; i < n; i++) {
      String[] line = SPACE.split(lines.get(i), 2);
      y[i] = Double.parseDouble(line[0]);

      String[] featureStrs = SPACE.split(line[1]);

      for (int j = 0; j < featureCount; j++) {
        x[i][j] = Double.parseDouble(featureStrs[j]);
      }
    }

    TrainingData td = new TrainingData(x, y);

    List<Tuple2<Double[], Double>> faList = 
      new ArrayList<Tuple2<Double[], Double>>();

    //for (int i = 0; i < n; i++) {
    for (int i = 0; i < 10; i++) {
      faList.add(new Tuple2<Double[], Double>(x[i], y[i]));
    }

    return new Tuple2<TrainingData, Iterable<Tuple2<Double[], Double>>>(
        td, faList);
  }

}

