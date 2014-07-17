package io.prediction.workflow

// DO-NOT-DELETE
/*
    // Workflow returns Array[Array[Model]] which are all models.
    // Extract Models for Deployment
    val models = evalAlgoModelMap.values.toArray.map { e =>
      e.map { m =>
        m._2 match {
          case rdd: RDD[_] => rdd.collect.head
          case ppm: PersistentParallelModel =>
            ppm.save("foobar")
            ppm
          case _ => Unit//ParallelModel(className = m._2.getClass.getName)
        }
      }.toArray
    }
*/
// END-OF-DO-NOT-DELETE

