We are creating an engine in PredictionIO for friend/item recommendation in social network settings. It learns from user profiles, item information, social interactions as well as past recommendation history and builds a model to provide suggestions on friend/item for users.

Two algorithms are implemented.

1. Random
2. Keyword Similarity KNN

To run the engine, you need to Register (Build) + Train + Deploy : 

```
$PIO_HOME/bin/pio register --engine-json $EngineJson

$PIO_HOME/bin/pio train --engine-json $EngineJson

$PIO_HOME/bin/pio deploy --engine-json $EngineJson
```

$EngineJson is

1. "random_engine.json" for "Random"
2. "keyword_similarity_engine.json" for "Keyword Similarity KNN"

To query :

```
curl -H "Content-Type: application/json" -d '{ "user": $UserId , "item" : $ItemId}' http://localhost:8000/queries.json
```

$UserId and $ItemId are the user and item you want to query.


To use random script:
```
$ python file_random.py [number of user] [number of item] 
```
expected files: data/user_profile.txt & data/item.txt
output files: data/mini_user_key_word.txt & data/mini_item.txt
