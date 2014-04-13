package io.prediction.algorithms.mahout.itemrec;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

/* based on Mahout GenericBooleanPrefUserBasedRecommender */
public class BooleanPrefUserBasedRecommender extends GenericUserBasedRecommender {

  private final FastByIDMap<FastIDSet> seenDataMap;

  public BooleanPrefUserBasedRecommender(DataModel dataModel,
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

  /**
   * This computation is in a technical sense, wrong, since in the domain of "boolean preference users" where
   * all preference values are 1, this method should only ever return 1.0 or NaN. This isn't terribly useful
   * however since it means results can't be ranked by preference value (all are 1). So instead this returns a
   * sum of similarities to any other user in the neighborhood who has also rated the item.
   */
  @Override
  protected float doEstimatePreference(long theUserID, long[] theNeighborhood, long itemID) throws TasteException {
    if (theNeighborhood.length == 0) {
      return Float.NaN;
    }
    DataModel dataModel = getDataModel();
    UserSimilarity similarity = getSimilarity();
    float totalSimilarity = 0.0f;
    boolean foundAPref = false;
    for (long userID : theNeighborhood) {
      // See GenericItemBasedRecommender.doEstimatePreference() too
      if (userID != theUserID && dataModel.getPreferenceValue(userID, itemID) != null) {
        foundAPref = true;
        totalSimilarity += (float) similarity.userSimilarity(theUserID, userID);
      }
    }
    return foundAPref ? totalSimilarity : Float.NaN;
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
    return "BooleanPrefUserBasedRecommender";
  }

}
