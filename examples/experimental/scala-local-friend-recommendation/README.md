##Our Engine
We are creating an engine in PredictionIO for friend/item recommendation in social network settings. It learns from user profiles, item information, social interactions as well as past recommendation history and builds a model to provide suggestions on friend/item for users.

#####Two algorithms are implemented:
1. Random
2. Keyword Similarity KNN

#####Expected data:
The dataset for KDD Cup 2012 Track 1 is required. <br />
The KDD cup page can be found <a href="https://www.kddcup2012.org/c/kddcup2012-track1">here</a><br />
The dataset are <a href="https://www.kddcup2012.org/c/kddcup2012-track1/data">here</a><br />
The below files are required to put into the *data* folder.

1. item.txt
2. user_profile.txt
3. user\_key\_word.txt
4. user_action.txt
5. user_sns.txt
6. rec\_log\_train.txt

#####Sampling a subset of data:
You can sample a subset of the data with *file_random.py*
```
python file_random $UserSize $ItemSize
```
*$UserSize* and *$ItemSize* are the sample sizes of users and items respectively.

Put the input files into the data folder

The program runs with files:

1. item.txt
2. user_profile.txt
3. user\_key\_word.txt
4. user\_action.txt
5. user\_sns.txt
6. rec\_log\_train.txt

And output files:

1. mini_item.txt
2. mini\_user\_profile.txt
3. mini\_user_key_word.txt
4. mini\_user_action.txt
5. mini\_user\_sns.txt
6. mini\_rec\_log_train.txt

After sampling, please set the file path parameters in *$EngineJson* (described below) to point to the output in order to use them.

#####Notice about Spark settings:
As the data set is large, we recommend setting spark memories to be large. Please set the below two lines with the two values *$E_M* and *$D_M* in the *$SPARK_HOME/conf/spark-defaults.conf*

1. spark.executor.memory *$E_M*
2. spark.driver.memory *$D_M*

We have tested "Random" and "Keyword Similarity KNN" algorithms with *$E_M* = 16g and *$D_M* = 16g.

#####To run the engine, you need to Build + Train + Deploy:
```
$PIO_HOME/bin/pio build -v $EngineJson

$PIO_HOME/bin/pio train -v $EngineJson

$PIO_HOME/bin/pio deploy -v $EngineJson
```

$EngineJson is

1. "random_engine.json" for "Random"
2. "keyword_similarity_engine.json" for "Keyword Similarity KNN"

Note: if the accesskey error rises when deploying, please set it to any dummy value and then the program will work.

#####To query:
```
curl -H "Content-Type: application/json" -d '{ "user": $UserId , "item" : $ItemId}' http://localhost:8000/queries.json
```

*$UserId* and *$ItemId* are the user and item you want to query.

#####Prediction provided:
Our local algorithm provides two predicted values as below for each user-item pair queried.

1. confidence (how confident the algorithm is to predict that the user will accept the item)
2. acceptance (when the confidence is high, the algorithm will predict that the user will accept the item)
