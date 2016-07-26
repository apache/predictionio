# E-Commerce Recommendation Template with rate event as training data.

This examples demonstrates how to modify E-Commerce Recommendation template to use "rate" event as Training Data.

However, recent "view" event is still used for recommendation for new user (to recommend items similar to what new user just recently viewed) and the returned scores are not predicted rating but a ranked scores for new user.

This template also supports that the user may rate same item multiple times and latest rating value will be used for training. The modification can be further simplified if the support of this case is not needed.

The modification is based on E-Commerce Recommendation template v0.1.1.

You can find the complete modified source code in `src/` directory.

## Documentation

Please refer to http://predictionio.incubator.apache.org/templates/ecommercerecommendation/quickstart/
and
http://predictionio.incubator.apache.org/templates/ecommercerecommendation/train-with-rate-event/

### import sample data

```
$ python data/import_eventserver.py --access_key <your_access_key>
```
