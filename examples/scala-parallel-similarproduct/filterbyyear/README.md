# Similar Product Template

## Documentation

Please refer to http://docs.prediction.io/templates/similarproduct/quickstart/

## Versions

### develop

### v0.1.1

- Persist RDD to memory (.cache()) in DataSource for better performance
- Use local model for faster serving.

### v0.1.0

- initial version


## Development Notes

### import sample data
Import movielens data using below repository
https://github.com/k4hoo/systest/tree/master/0.8/ml100k/demo-movielens

```
$ python -m batch_import <Access Key> http://127.0.0.1:7070
```

### Goal: sample code for the recommendation engine not to recommend movies prior to 1990

Create new engine and install movie lens 100k data which is in below format:

UserID::MovieID::Rating::Timestamp

### Changes to Engine.scala

Added “recommendFromYear” attribute to the Query class. we can pass the “recommendFromYear” attribute from the query request.
```
case class Query(
  items: List[String],
  num: Int,
  categories: Option[Set[String]],
  whiteList: Option[Set[String]],
  blackList: Option[Set[String]],
  recommendFromYear: Option[Int]
) extends Serializable

    2)  Added “year” attribute to the class ItemScore. 

case class ItemScore(
  item: String,
  score: Double,
  year: Int
) extends Serializable

```
### Changes to DataSource.scala

Added attribute “year” to the class Item
```
      1) 	case class Item(categories: Option[List[String]],year: Int)

      2) 	In the eventsDb.aggregateProperties, adding year property

  Item(categories = properties.getOpt[List[String]]("categories"),year = properties.get[Int]("year"))
```
### Changes to ALSAlgorihm.scala

In the predict method, passing “recommendFromYear” attribute to the isCandidateItem method
```
		 isCandidateItem(
        i = i,
        items = model.items,
        categories = query.categories,
        queryList = queryList,
        whiteList = whiteList,
        blackList = blackList,
        recommendFromYear = query.recommendFromYear
     		 )


In “isCandidateItem” method, verifying if Item’s year is greater than “recommendFromYear” attribute.
		
		 private  def isCandidateItem(
    i: Int,
    items: Map[Int, Item],
    categories: Option[Set[String]],
    queryList: Set[Int],
    whiteList: Option[Set[Int]],
    blackList: Option[Set[Int]],
    recommendFromYear: Option[Int]
  ): Boolean = {
    whiteList.map(_.contains(i)).getOrElse(true) &&
    blackList.map(!_.contains(i)).getOrElse(true) &&
    // discard items in query as well
    (!queryList.contains(i)) &&
    // filter categories
    items(i).year > recommendFromYear.getOrElse(1) &&
    categories.map { cat =>
      items(i).categories.map { itemCat =>
        // keep this item if has ovelap categories with the query
        !(itemCat.toSet.intersect(cat).isEmpty)
      }.getOrElse(false) // discard this item if it has no categories
    }.getOrElse(true)
  }


	


3)  In the predict method, returning year as well as part of ItemScore


    val itemScores = topScores.map { case (i, s) =>
      new ItemScore(
        item = model.itemIntStringMap(i),
        score = s,
        year = model.items(i).year
      )
    }

    new PredictedResult(itemScores)


```

### Example Request

```
curl -H "Content-Type: application/json" \
-d '{ "items": ["171"], "num": 10, "recommendFromYear":1990 }' \
http://localhost:8000/queries.json
```
