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

package org.apache.predictionio.examples.java.recommendations.tutorial5;

import org.apache.predictionio.controller.java.LJavaAlgorithm;
import org.apache.predictionio.controller.java.EmptyParams;
import org.apache.predictionio.examples.java.recommendations.tutorial1.TrainingData;
import org.apache.predictionio.examples.java.recommendations.tutorial1.Query;
import org.apache.predictionio.engines.util.MahoutUtil;

import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.common.TasteException;
import scala.Tuple4;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Simple Mahout ItemBased Algorithm integration for demonstration purpose */
public class MahoutAlgorithm extends
  LJavaAlgorithm<EmptyParams, TrainingData, MahoutAlgoModel, Query, Float> {

  final static Logger logger = LoggerFactory.getLogger(MahoutAlgorithm.class);

  MahoutAlgoParams params;

  public MahoutAlgorithm(MahoutAlgoParams params) {
    this.params = params;
  }

  @Override
  public MahoutAlgoModel train(TrainingData data) {
    List<Tuple4<Integer, Integer, Float, Long>> ratings = new ArrayList<
      Tuple4<Integer, Integer, Float, Long>>();
    for (TrainingData.Rating r : data.ratings) {
      // no timestamp
      ratings.add(new Tuple4<Integer, Integer, Float, Long>(r.uid, r.iid, r.rating, 0L));
    }
    DataModel dataModel = MahoutUtil.jBuildDataModel(ratings);
    return new MahoutAlgoModel(dataModel, params);
  }

  @Override
  public Float predict(MahoutAlgoModel model, Query query) {
    float predicted;
    try {
      predicted = model.getRecommender().estimatePreference((long) query.uid, (long) query.iid);
    } catch (TasteException e) {
      predicted = Float.NaN;
    }
    return predicted;
  }

}
