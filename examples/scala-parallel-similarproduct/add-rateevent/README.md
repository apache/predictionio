# Similar Product Template Modified to Add Explicit Rate Event

This example engine is based on Similar Product Tempplate version v0.1.2  and is modified to add Explicit Rate Event to training data.

For example, An User would rate an item with a score or rating.The rating is used to train the model.   

## Documentation

Please refer to http://docs.prediction.io/templates/similarproduct/quickstart/



## Development Notes

### Changes to Preparator.scala

1) "rateEventsRDD" is initialized by filtering events of type "rate" from Events database.

```
   val rateEventsRDD: RDD[RateEvent] = eventsDb.find(
        appId = dsp.appId,
        entityType = Some("user"),
        eventNames = Some(List("rate")...
       
   val rateEvent = try {
        event.event match {
        case "rate" => RateEvent(...
```



### Changes to Preparator.scala

1) val "rateEvents" is added to class "PreparedData"

```
   class PreparedData(
    val users: RDD[(String, User)],
    val items: RDD[(String, Item)],
    val rateEvents: RDD[RateEvent]
```

2) val "rateEvents" is initialized in the Object of class "PreparedData".

```
   new PreparedData(
    users = trainingData.users,
    items = trainingData.items,
    rateEvents = trainingData.rateEvents)
```

### Changes to ALSAlgorithm.scala

1) Changed the Signature of "train" method  to include SparkContext and edited  method definition to specify the   debug message.

```
   def train(sc:SparkContext ,data: PreparedData): ALSModel = {
   require(!data.rateEvents.take(1).isEmpty,
   s"rateEvents in PreparedData cannot be empty." +
```

2) MlibRatings are initialized from rateEvents.

```
   val mllibRatings = data.rateEvents
```

3) Invoke "ALS.train" method to train explicit rate events.
```
   val m = ALS.train(
    ratings = mllibRatings,
    rank = ap.rank,
    iterations = ap.numIterations,
    lambda = ap.lambda,
    blocks = -1,
    seed = seed)
```

4) Define "rateEvent"  RDD to filter rate events from events.

```
   val rateEventsRDD: RDD[RateEvent] = eventsDb.find(...)
```

5) Add "rateEvent"  to class "TrainingData".

```
   class TrainingData(
   val users: RDD[(String, User)],
   val items: RDD[(String, Item)],
   val rateEvents: RDD[RateEvent]
 )
```

6) Add "rateEvent"  to object "TrainingData".

```
   new TrainingData(
    users = usersRDD,
    items = itemsRDD,
    rateEvents = rateEventsRDD)
```



