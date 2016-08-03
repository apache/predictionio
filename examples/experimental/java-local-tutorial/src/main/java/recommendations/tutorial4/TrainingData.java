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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TrainingData implements Serializable {
  public final List<Rating> ratings;
  public final List<String> genres;
  public final Map<Integer, String[]> itemInfo;
  public final Map<Integer, String[]> userInfo;

  public TrainingData(List<Rating> ratings, List<String> genres, Map<Integer, String[]> itemInfo,
      Map<Integer, String[]> userInfo) {
    this.ratings = ratings;
    this.genres = genres;
    this.itemInfo = itemInfo;
    this.userInfo = userInfo;
  }
  
  public TrainingData(TrainingData data) {
    ratings = data.ratings;
    genres = data.genres;
    itemInfo = data.itemInfo;
    userInfo = data.userInfo;
  }

  @Override
  public String toString() {
    return "TrainingData: rating.size=" + ratings.size() + " genres.size=" + genres.size()
      + " itemInfo.size=" + itemInfo.size() + " userInfo.size=" + userInfo.size();
  }

  public static class Rating implements Serializable {
    public int uid; // user ID
    public int iid; // item ID
    public float rating;

    public Rating(int uid, int iid, float rating) {
      this.uid = uid;
      this.iid = iid;
      this.rating = rating;
    }

    @Override
    public String toString() {
      return "(" + uid + "," + iid + "," + rating + ")";
    }
  }
}
