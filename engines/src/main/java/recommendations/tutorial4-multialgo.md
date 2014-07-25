# Tutorial 4 - Multiple Algorithms Engine

At this point you have already tasted a sense of implementing, deploying, and evaluating a recommendation system with collaborative filtering techniques. However, this technique suffers from a cold-start problem where new items have no user action history. In this tutorial, we introduce a feature-based recommendation technique to remedy this problem by constructing a user-profile for each users. In addition, Prediction.IO infrastructure allows you to combine multiple recommendation systems together in a single engine. For a history-rich items, the engine can use results from the collaborative filtering algorithm, and for history-absent items, the engine returns prediction from the feature-based recommendation algorithm, moreover, we can ensemble multiple predictions too.

This tutorial guides you toward incorporating a feature-based algorithm into the existing CF-recommendation engine introduced in tutorial 1, 2, and 3.
