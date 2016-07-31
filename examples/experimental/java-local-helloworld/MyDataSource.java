/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sample.java.helloworld;

import org.apache.predictionio.controller.java.*;

import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyDataSource extends LJavaDataSource<
  EmptyDataSourceParams, EmptyDataParams, MyTrainingData, MyQuery, EmptyActualResult> {

  final static Logger logger = LoggerFactory.getLogger(MyDataSource.class);

  @Override
  public MyTrainingData readTraining() {
    List<MyTrainingData.DayTemperature> temperatures =
      new ArrayList<MyTrainingData.DayTemperature>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader("../data/helloworld/data.csv"));
      String line;
      while ((line = reader.readLine()) != null) {
        String[] tokens = line.split(",");
        temperatures.add(
          new MyTrainingData.DayTemperature(tokens[0], Double.parseDouble(tokens[1])));
      }
      reader.close();
    } catch (Exception e) {
      logger.error("Caught Exception: " + e.getMessage());
      System.exit(1);
    }

    return new MyTrainingData(temperatures);
  }
}
