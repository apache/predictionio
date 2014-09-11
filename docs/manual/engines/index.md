---
layout: docs
title: Engines
---

# Engines

Each engine represents a type of prediction. 

## Getting Started with Engine Instance

Let say you want to launch an instance of Item Recommendation Engine for product recommendation.

### Specific which App the instance is targeted to

```
(TODO)
```

### Launching an Engine Instance
```
$ cd $PIO_HOME/engines/item-rec
$ ../../bin/pio train
$ ../../bin/pio <TODO>
```
You may launch more than one engine instance in PredictionIO.

If it is your first time using PredictionIO, these [tutorials and samples](/tutorials/engines/) should be helpful.


## Schedule Model Re-training

You may set up a crontab in Linux to update the predictive model with new data regularly. For example, to run the re-training every 6 hours:

```
$ crontab -e

0 */6 * * *     cd <PIO_HOME>/engines/item-rec; ../../bin/pio train; ../../bin/pio deploy  
```
where `<PIO_HOME>` is your installation path of PredictionIO.

## Built-in Engines

PredictionIO comes with the following engines. 

* [Item Ranking](/engines/itemrank)
* [Item Recommendation](/engines/itemrec)
* [Item Similarity](/engines/itemsim)

You may start with these [tutorials and samples](tutorials.html).

## Building your own Engine

Please read the [Engine Builders' Guide](/enginebuilders/) for details.