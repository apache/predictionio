package io.prediction.engines.java.recommendations.multialgo;

import java.util.Arrays;
import io.prediction.controller.java.LJavaDataSource;
import io.prediction.controller.EmptyParams;
import scala.Tuple2;
import scala.Tuple3;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Iterable;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.FileReader;
import java.io.BufferedReader;


public class DataSource extends LJavaDataSource<
  DataSourceParams, EmptyParams, TrainingData, Query, Object> {

  final static Logger logger = LoggerFactory.getLogger(DataSource.class);

  DataSourceParams params;

  public DataSource(DataSourceParams params) {
    this.params = params;
  }

  public List<String[]> readFile(String filepath, String delimiter) {
    List<String[]> tokensList = new ArrayList<String[]>();
    try {
      List<String> lines = new ArrayList<String>();
      BufferedReader in = new BufferedReader(new FileReader(filepath));

      while (in.ready()) {
        String s = in.readLine();
        lines.add(s);
      }
      in.close();

      for (String line: lines) {
        String[] tokens = line.split(delimiter);
        tokensList.add(tokens);
      }

    } catch (FileNotFoundException e) {
      logger.error("Caught FileNotFoundException " + e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      logger.error("Can't parse file. Caught Exception: " + e.getMessage() 
          + "Trace: " + Arrays.toString(e.getStackTrace()));
      System.exit(1);
    }

    return tokensList;
  }

  public List<TrainingData.Rating> getRatings() {
    List<TrainingData.Rating> ratings = new ArrayList<TrainingData.Rating>();
    for (String[] tokens: readFile(params.dir + "u.data", "[\t,]")) {
      TrainingData.Rating rating = new TrainingData.Rating(
          Integer.parseInt(tokens[0]),
          Integer.parseInt(tokens[1]),
          Float.parseFloat(tokens[2]));
      ratings.add(rating);
    }
    return ratings;
  }

  public List<String> getGenres() {
    List<String> genres = new ArrayList<String>();
    for (String[] tokens: readFile(params.dir + "u.genre", "[\t,]")) {
      if (!tokens[0].equals("")) {
        genres.add(tokens[0]);
      }
    }
    return genres;
  }

  public Map<Integer, String[]> getItemInfo() {
    List<String[]> tokensList = readFile(params.dir + "u.item", "[\\|]");
    Map<Integer, String[]> itemInfo = new HashMap <> ();
    for (String[] tokens : tokensList) {
      itemInfo.put(Integer.parseInt(tokens[0]), tokens);
    }
    return itemInfo;
  }
  
  public Map<Integer, String[]> getUserInfo() {
    List<String[]> tokensList = readFile(params.dir + "u.user", "\\|");
    Map<Integer, String[]> userInfo = new HashMap <> ();
    for (String[] tokens : tokensList) {
      userInfo.put(Integer.parseInt(tokens[0]), tokens);
    }
    return userInfo;
  }

  @Override
  public Iterable<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>> read() {
    List<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>> data =
      new ArrayList<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>>();

    data.add(new Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>(
      new EmptyParams(),
      new TrainingData(getRatings(), getGenres(), getItemInfo(), getUserInfo()),
      new ArrayList<Tuple2<Query, Object>>()
    ));

    return data;
  }

}
