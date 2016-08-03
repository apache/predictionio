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

package org.apache.predictionio.examples.java.recommendations.tutorial4;

import org.apache.predictionio.controller.java.LJavaAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

public class FeatureBasedAlgorithm 
  extends LJavaAlgorithm<
      FeatureBasedAlgorithmParams, PreparedData, FeatureBasedModel, Query, Float> {
  public final FeatureBasedAlgorithmParams params;
  final static Logger logger = LoggerFactory.getLogger(FeatureBasedAlgorithm.class);

  public FeatureBasedAlgorithm(FeatureBasedAlgorithmParams params) {
    this.params = params;
  }

  public FeatureBasedModel train(PreparedData data) {
    Map<Integer, RealVector> userFeatures = new HashMap<Integer, RealVector>();
    Map<Integer, Integer> userActions = new HashMap<Integer, Integer>();

    for (Integer uid : data.userInfo.keySet()) {
      userFeatures.put(uid, new ArrayRealVector(data.featureCount));
      userActions.put(uid, 0);
    }

    for (TrainingData.Rating rating : data.ratings) {
      final int uid = rating.uid;
      final int iid = rating.iid;
      final double rate = rating.rating;

      // Skip features outside the range.
      if (!(params.min <= rate && rate <= params.max)) continue;

      final double actualRate = (rate - params.drift) * params.scale;
      final RealVector userFeature = userFeatures.get(uid);
      final RealVector itemFeature = data.itemFeatures.get(iid);
      userFeature.combineToSelf(1, actualRate, itemFeature);

      userActions.put(uid, userActions.get(uid) + 1);
    }

    // Normalize userFeatures by l-inf-norm
    for (Integer uid : userFeatures.keySet()) {
      final RealVector feature = userFeatures.get(uid);
      feature.mapDivideToSelf(feature.getLInfNorm());
    }

    // Normalize itemFeatures by weight
    Map<Integer, RealVector> itemFeatures = new HashMap<Integer, RealVector>();
    for (Integer iid : data.itemFeatures.keySet()) {
      final RealVector feature = data.itemFeatures.get(iid);
      final RealVector normalizedFeature = feature.mapDivide(feature.getL1Norm());
      itemFeatures.put(iid, normalizedFeature);
    }
    
    return new FeatureBasedModel(userFeatures, userActions, itemFeatures);
  }

  public Float predict(FeatureBasedModel model, Query query) {
    final int uid = query.uid;
    final int iid = query.iid;

    if (!model.userFeatures.containsKey(uid)) {
      return Float.NaN;
    }

    if (!model.itemFeatures.containsKey(iid)) {
      return Float.NaN;
    }

    final RealVector userFeature = model.userFeatures.get(uid);
    final RealVector itemFeature = model.itemFeatures.get(iid);
    
    return new Float(userFeature.dotProduct(itemFeature));
  }
}

