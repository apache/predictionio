---
layout: docs
title: Tutorials and Samples
---

# Engine Tutorials and Samples

## Item Ranking

* (coming soon)

## Item Recommendation

### Importing Movie-Lens Data

Clone our
[Python-SDK](https://github.com/PredictionIO/PredictionIO-Python-SDK) and
switch to develop branch to get the latest changes.

```
$ git clone https://github.com/PredictionIO/PredictionIO-Python-SDK.git
$ git checkout develop
```

Download Movie-Lens data

```
$ curl -o ml-100k.zip http://www.grouplens.org/system/files/ml-100k.zip 
$ unzip ml-100k.zip
```

Import data. The import script takes two parameters: `<app_id> <url>`. `<app_id>` is an integer
identifies your address space; `<url>` is the EventServer url (default:
http://localhost:7070)

```
$ py -m examples.demo-movielens.batch_import <app_id> http://localhost:7070
```

The import takes a minute or two. At the end you should see the following
output:

```
{u'status': u'alive'}
[Info] Initializing users...
[Info] 943 users were initialized.
...
[Info] Importing rate actions to PredictionIO...
[Info] 100000 rate actions were imported.
```

> You may delete *all* data belonging to a specific `<app_id>` with this request.
> There is no way to undo this delete, use it cautiously!
```
$ curl -i -X DELETE http://localhost:7070/events.json?appId=<app_id>
```

### Deploying the Item Recommendation engine
* (coming soon)

## Item Similarity

* (coming soon)
