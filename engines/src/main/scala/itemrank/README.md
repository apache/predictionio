ItemRank engine
===============

## Use case

In each day (or other time unit), there is a list of items to be shown to users and we want to personalize the order of the list of items for each user.

## Prediction Input

- user id
- list of items to be ranked

## Prediction Output

- ranked items with score


## App Data

* Users
* Items
* U2IActions
* ItemSets - the list of items at each day (or other time unit)

Import Sample data
==================

## Start Mongo

	$ mongod

## Run

	$ sbt/sbt "engines/runMain io.prediction.engines.itemrank.CreateSampleData"


By default, it imports data into mongo with appid=1 and create 30 days of sample data. You may specify different appid or different number of days by using --appid and --days parameters.

For example, to import to appid=4 with 90 days of data:

	$ sbt/sbt "engines/run io.prediction.engines.itemrank.CreateSampleData --appid 4 --days 90"

