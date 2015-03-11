# Recommendation Template With Filtering by Category

This engine template is based on the Recommendation Template version v0.1.2. It's modified so that each item has a set
of categories, queries include a `category` field and results only include items in the given category.

## Documentation

Please refer to http://docs.prediction.io/templates/recommendation/quickstart/

## Development Notes

### Sample data

This example uses two different files from the MovieLens 100k data. [Download](http://files.grouplens.org/datasets/movielens/ml-100k.zip)

The first file is the ratings data file, which is a tab separated file. The first 3 fields are used: user id, item id
and rating.

The second file is the items data file, in where different fields are separated by the `|` character. The fields of
interest are the first one (item id) and 19 category fields, starting at the 6th one and going till the end. Each
of these fields has the value `1` if the item is the given category, and `0` otherwise.

### Importing sample data

The `data/import_eventserver.py` script allows importing the sample data. Valid arguments are:

* `--url`: Event server URL
* `--access_key` App access key
* `--ratings_file`: Ratings file path
* `--items_file`: Items file path

### Changes to Engine.scala

Added `category` field to the `Query` class:

```scala
case class Query(
  user: String,
  num: Int,
  category: String
) extends Serializable
```

### Changes to DataSource.scala

* Introduced class `Item`:

```scala
case class Item(
  id: String,
  categories: List[String]
)
```

* Modified class `TrainingData` to include items:

```scala
class TrainingData(
  val items: RDD[Item],
  val ratings: RDD[Rating]
) extends Serializable {
  override def toString = {
    s"items: [${items.count()}] (${items.take(2).toList}...)" +
    s" ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}
```

* Modified `readTraining` function so that it loads items:

```scala
    ...
    val itemsRDD: RDD[Item] = eventsDb.aggregateProperties(
      appId = dsp.appId,
      entityType = "item"
    )(sc).map {
      case (entityId, properties) =>
        try {
          Item(id = entityId, categories = properties.get[List[String]]("categories"))
        } catch {
          case e: Exception =>
            logger.error(s"Failed to get properties ${properties} of" +
              s" item ${entityId}. Exception: ${e}.")
            throw e
        }
    }
    ...
```

### Changes to Preparator.scala

* Added `items` field to class `PreparedData`. Pass `items` value from training data.

```scala
class PreparedData(
  val items: RDD[Item],
  val ratings: RDD[Rating]
) extends Serializable
```

```scala
    new PreparedData(items = trainingData.items, ratings = trainingData.ratings)
```

### Changes to ALSModel.scala

* Introduce `CategoriesALSModels` class. This class maintains a reference to an `ALSModel` for each different category.
It also maintains a reference to `userStringIntMap` and `itemStringIntMap`, which are used when predicting queries of
any category:

```scala
class CategoriesALSModels(
    val modelsMap: Map[String, ALSModel],
    val userStringIntMap: BiMap[String, Int],
    val itemStringIntMap: BiMap[String, Int])
  extends IPersistentModel[ALSAlgorithmParams] {
  def save(id: String, params: ALSAlgorithmParams, sc: SparkContext): Boolean = {
    sc.parallelize(modelsMap.keys.toSeq)
      .saveAsObjectFile(s"/tmp/${id}/mapKeys")
    sc.parallelize(Seq(userStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/userStringIntMap")
    sc.parallelize(Seq(itemStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/itemStringIntMap")

    modelsMap.foreach {
      case (category, model) =>
        model.save(s"${id}-${category}", params, sc)
    }
    true
  }

  override def toString = {
    s"categories: [${modelsMap.size}]" +
    s"(${modelsMap.keys.take(2)}...})" +
    s" userStringIntMap: [${userStringIntMap.size}]" +
    s"(${userStringIntMap.take(2)}...)" +
    s" itemStringIntMap: [${itemStringIntMap.size}]" +
    s"(${itemStringIntMap.take(2)}...)"
  }
}

object CategoriesALSModels
  extends IPersistentModelLoader[ALSAlgorithmParams, CategoriesALSModels] {
  def apply(id: String, params: ALSAlgorithmParams, sc: Option[SparkContext]) = {
    val keys = sc.get
      .objectFile[String](s"/tmp/${id}/mapKeys").collect().toList

    val modelMap = keys.map { category =>
      category -> ALSModel.apply(s"${id}-${category}", params, sc)
    }.toMap

    val userStringIntMap = sc.get
      .objectFile[BiMap[String, Int]](s"/tmp/${id}/userStringIntMap").first
    val itemStringIntMap = sc.get
      .objectFile[BiMap[String, Int]](s"/tmp/${id}/itemStringIntMap").first

    new CategoriesALSModels(modelMap, userStringIntMap, itemStringIntMap)
  }
}
```

### Changes to ALSAlgorithm.scala

* Find set of categories

```scala
    ...
    val categories = data.items.flatMap(_.categories).distinct().collect().toSet
    ...
```

* Create an ALS model for each category

```scala
    ...
    val categoriesMap = categories.map { category =>
      val itemIds = data.items
        .filter(_.categories.contains(category))
        .map(item => itemStringIntMap(item.id))
        .collect()
        .toSet

      val itemFeatures = m.productFeatures.filter {
        case (id, features) => itemIds.contains(id)
      }

      category -> new ALSModel(
        rank = m.rank,
        userFeatures = m.userFeatures,
        productFeatures = itemFeatures
      )
    }.toMap
    ...
```

* Create a `CategoriesALSModels` object:

```scala
    ...
    new CategoriesALSModels(
      modelsMap = categoriesMap,
      userStringIntMap = userStringIntMap,
      itemStringIntMap = itemStringIntMap
    )
```

* Select model based on the query category, return an empty result if the category does not exist:

```scala
  def predict(models: CategoriesALSModels, query: Query): PredictedResult = {
    models.modelsMap.get(query.category).map { model =>
      // Convert String ID to Int index for Mllib
      models.userStringIntMap.get(query.user).map { userInt =>
        // create inverse view of itemStringIntMap
        val itemIntStringMap = models.itemStringIntMap.inverse
        // recommendProducts() returns Array[MLlibRating], which uses item Int
        // index. Convert it to String ID for returning PredictedResult
        val itemScores = model.recommendProducts(userInt, query.num)
          .map (r => ItemScore(itemIntStringMap(r.product), r.rating))
        PredictedResult(itemScores)
      }.getOrElse{
        logger.info(s"No prediction for unknown user ${query.user}.")
        PredictedResult(Array.empty)
      }
    }
    .getOrElse {
      logger.info(s"No prediction for unknown category ${query.category}.")
      PredictedResult(Array.empty)
    }
  }
```

### Example Request

The script `data/send_query.py` has been modified to represent the updated query structure:

```python
print engine_client.send_query({"user": "1", "num": 4, "category": "action"})
```