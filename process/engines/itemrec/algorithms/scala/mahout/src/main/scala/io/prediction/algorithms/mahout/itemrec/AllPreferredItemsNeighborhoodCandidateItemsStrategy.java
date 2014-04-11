package io.prediction.algorithms.mahout.itemrec;

import org.apache.mahout.cf.taste.impl.recommender.AbstractCandidateItemsStrategy;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;

/* modified based on PreferredItemsNeighborhoodCandidateItemsStrategy */

public final class AllPreferredItemsNeighborhoodCandidateItemsStrategy extends AbstractCandidateItemsStrategy {

  //private final DataModel seenDataModel;
  private final FastByIDMap<FastIDSet> seenDataMap;
  /*
   * @param seenDataModel set this to null if don't exclude seen items
   */
  public AllPreferredItemsNeighborhoodCandidateItemsStrategy(
    DataModel seenDataModel) throws TasteException {
      super();
      if (seenDataModel != null)
        this.seenDataMap = GenericBooleanPrefDataModel.toDataMap(
          seenDataModel);
      else
        this.seenDataMap = null;
  }

  public AllPreferredItemsNeighborhoodCandidateItemsStrategy()
    throws TasteException {
    this(null);
  }

  @Override
  public FastIDSet getCandidateItems(long userID,
    PreferenceArray preferencesFromUser, DataModel dataModel)
    throws TasteException {
      long[] seenItemIDs;
      if (this.seenDataMap != null) {
        FastIDSet ids = seenDataMap.get(userID);
        if (ids != null) {
          seenItemIDs = ids.toArray();
        } else {
          seenItemIDs = null;
        }
      } else {
        seenItemIDs = null;
      }
    return doGetCandidateItems(preferencesFromUser.getIDs(),
      dataModel, seenItemIDs);
  }

  /**
   * returns all items that have not been rated by the user and that were preferred by another user
   * that has preferred at least one item that the current user has preferred too.
   * excluding seen items.
   */
  protected FastIDSet doGetCandidateItems(long[] preferredItemIDs,
    DataModel dataModel, long[] seenItemIDs) throws TasteException {
    FastIDSet possibleItemsIDs = new FastIDSet();
    for (long itemID : preferredItemIDs) {
      PreferenceArray itemPreferences = dataModel.getPreferencesForItem(itemID);
      int numUsersPreferringItem = itemPreferences.length();
      for (int index = 0; index < numUsersPreferringItem; index++) {
        possibleItemsIDs.addAll(
          dataModel.getItemIDsFromUser(itemPreferences.getUserID(index)));
      }
    }
    if (seenItemIDs != null)
      possibleItemsIDs.removeAll(seenItemIDs);
    return possibleItemsIDs;
  }

  @Override
  protected FastIDSet doGetCandidateItems(long[] preferredItemIDs,
    DataModel dataModel) throws TasteException {
      return doGetCandidateItems(preferredItemIDs, dataModel, null);
  }

}
