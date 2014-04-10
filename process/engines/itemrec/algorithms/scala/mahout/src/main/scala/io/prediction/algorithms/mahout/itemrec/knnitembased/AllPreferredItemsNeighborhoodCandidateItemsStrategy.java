package io.prediction.algorithms.mahout.itemrec.knnitembased;

import org.apache.mahout.cf.taste.impl.recommender.AbstractCandidateItemsStrategy;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;

/* modified based on PreferredItemsNeighborhoodCandidateItemsStrategy */

public final class AllPreferredItemsNeighborhoodCandidateItemsStrategy extends AbstractCandidateItemsStrategy {
  /**
   * returns all items that have not been rated by the user and that were preferred by another user
   * that has preferred at least one item that the current user has preferred too
   */
  @Override
  protected FastIDSet doGetCandidateItems(long[] preferredItemIDs, DataModel dataModel) throws TasteException {
    FastIDSet possibleItemsIDs = new FastIDSet();
    for (long itemID : preferredItemIDs) {
      PreferenceArray itemPreferences = dataModel.getPreferencesForItem(itemID);
      int numUsersPreferringItem = itemPreferences.length();
      for (int index = 0; index < numUsersPreferringItem; index++) {
        possibleItemsIDs.addAll(dataModel.getItemIDsFromUser(itemPreferences.getUserID(index)));
      }
    }
    //possibleItemsIDs.removeAll(preferredItemIDs);
    return possibleItemsIDs;
  }

}
