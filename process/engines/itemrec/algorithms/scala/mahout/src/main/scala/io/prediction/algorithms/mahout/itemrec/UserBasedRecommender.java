package io.prediction.algorithms.mahout.itemrec;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

/* based on Mahout GenericUserBasedRecommender */
public class UserBasedRecommender extends GenericUserBasedRecommender {

  private final FastByIDMap<FastIDSet> seenDataMap;

  public UserBasedRecommender(DataModel dataModel,
    UserNeighborhood neighborhood,
    UserSimilarity similarity,
    DataModel seenDataModel)
    throws TasteException {
      super(dataModel, neighborhood, similarity);
      if (seenDataModel != null)
        this.seenDataMap = GenericBooleanPrefDataModel.toDataMap(
          seenDataModel);
      else
        this.seenDataMap = null;
  }


  @Override
  protected FastIDSet getAllOtherItems(long[] theNeighborhood, long theUserID) throws TasteException {
    DataModel dataModel = getDataModel();
    FastIDSet possibleItemIDs = new FastIDSet();
    for (long userID : theNeighborhood) {
      possibleItemIDs.addAll(dataModel.getItemIDsFromUser(userID));
    }

    // exclude seen items if seenDataMap != null
    if (this.seenDataMap != null) {
      FastIDSet ids = seenDataMap.get(theUserID);
      if (ids != null) {
        possibleItemIDs.removeAll(ids);
      }
    }

    return possibleItemIDs;
  }

  @Override
  public String toString() {
    return "UserBasedRecommender";
  }

}
