# Filtering result set on custom item field (Recommendation)

## Import data to pio engine

By default the recommendation template reads the rate and buy user events and the user itself. You can modify the default DataSource to read your custom item with specified list of properties.

First off all you have to import your events to the pio event server.

You can use ImportDataScript.scala to import users, movies and rate events from [movielenses database](http://grouplens.org/datasets/movielens/). 
Make sure that data files are in `UTF-8` encoding.

This command line tool accepts 2 args:

 1. app access key and it is mandatory
 2. pio engine url. default is `http://localhost:7070`
 
For example in the sbt console: `> runMain org.template.recommendation.ImportDataScript.scala <access_key>`

## Modify the engine.

This example is based on v0.1.1 of [scala parallel recommendation template](https://github.com/PredictionIO/template-scala-parallel-recommendation/)

In this example we modify DataSource to read custom event with one property.

* Modify the Query data structure with your needs, at this example we added item creation year as a query/filter param:

`case class Query(user: String, num: Int, creationYear: Option[Int] = None)`

* Define the Item case class which will describe your data:

`case class Item(creationYear: Option[Int])`

* Define the utility unmarshaller to read the list of properties to your structure:

```
object ItemMarshaller {
 def unmarshall(properties: DataMap): Option[Item] =
   Some(Item(properties.getOpt[Int]("creationYear")))
}
```

* Modify the TrainingData class to hold your custom items:

`class TrainingData(val ratings: RDD[Rating], val items: RDD[(String, Item)])`
 
* Modify the readTraining method in the DataSource to read your custom item:

```
val itemsRDD = eventsDb.aggregateProperties(
    appId = dsp.appId,
    entityType = "item"
  )(sc).flatMap { case (entityId, properties) ⇒
    ItemMarshaller.unmarshall(properties).map(entityId → _)
  }
```

* Modify the ASLModel to hold your custom items to have possibility filter them.

```
class ALSModel(
 val productFeatures: RDD[(Int, Array[Double])],
 val itemStringIntMap: BiMap[String, Int],
 val items: Map[Int, Item])
```


* Modify train method in ALSAlgorithm to match you items with numeric ids that are needed by the algo.

```
val items: Map[Int, Item] = data.items.map { case (id, item) ⇒
   (itemStringIntMap(id), item)
}.collectAsMap.toMap
```

* Define the filterItems method in ALSAlgorithm to filter the predicted result set according the query.

```
private def filterItems(selectedScores: Array[(Int, Double)],
                       items: Map[Int, Item],
                       query: Query) =
 selectedScores.view.filter { case (iId, _) ⇒
   items(iId).creationYear.map(icr ⇒ query.creationYear.forall(icr >= _))
     .getOrElse(true)
 }
```

* And the last step will be to modify the predict method in the ALSAlgorithm to filter predicted result set:

`val filteredScores = filterItems(indexScores, model.items, query)`

* Now you can filter your recommendation by items that were made after some certain year:

`curl -H 'Content-Type: application/json' '127.0.0.1:8000/queries.json' -d '{"user":100, "num":5, "creationYear":1990}' | python -m json.tool`

That's it! Now your recommendation engine is using filtering on custom item field on predicted result set.


