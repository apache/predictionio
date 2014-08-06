## How to run an engine (evaluation)

There are three main components:
- PredictionIO core library jar
- External dependencies jar
- Engine jar (for engine builders, this is where you update your code)

The first two jar only requires one time compile, while the engine jar needs to be re-compiled everytime you change the code.

### Compiling external dependencies
Only need to do this once, unless you have updated the external dependency jar. Assuming you are at the repository root.
```
$ sbt/sbt "project engines" assemblyPackageDependency
```
You will find the external dependencies all packed in a fat jar at `engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar`.

### Compiling your engine (as well as other PIO libraries)
Everytime you change your code, you need to recompile the engine with the following command. Don't need to worry too much about incremental compilations, as sbt will take care of it.
```
$ sbt/sbt package
```

### Start Spark cluster
PredictionIO leverages the Spark infrastructure. In this example, we will use the standalone mode. We only cover the basic info. Please go to [Spark documentation](http://spark.apache.org/docs/latest/spark-standalone.html) for detailed explanation.

Start master server:
```
$ ./sbin/start-master.sh
```

You should be able to find the webui in [http://localhost:8080](http://localhost:8080). Now, we add workers to the master:
```
$ ./bin/spark-class org.apache.spark.deploy.worker.Worker spark://IP:PORT
```
where you can find `spark://IP:PORT` in the master UI page. Once you have started the worker, you should be able to see something under the "Workers" table in UI page.


### Running an engine
Let's try to run the example regression engine. You can find the code from [this link](https://github.com/PredictionIO/Imagine/blob/master/engines/src/main/scala/regression/Run.scala):

The following command kick starts the evaluation workflow for the regression engine. Change two things:
- Replace `spark://Justins-MacBook-Pro.local:7077` by your spark master url.
- Replace `io.prediction.examples.regression.Runner` with the main runner class of your engine. Don't have to change for the current example.
```
$ spark-submit --verbose \
--jars engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar \
--class "io.prediction.examples.regression.Runner" \
--master spark://Justins-MacBook-Pro.local:7077 \
core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar
```

You will see a lot of logging messages, and towards the end, you will see `MSE: 25169010958286325000000000000000000000.000000`, the number can be a whatever large number. It is the output of the evaluation workflow, and MSE stands for mean-square-error. This large error is expected, because in the default program, it doesn't have enough iteration to coverage to a point where the regression gives resonable output.

As an exercise, please go to the runner main file and find the parameter which defines the number of iterations for the regression. After updating the program, recompile the engine with `sbt/sbt package`, and run the above `spark-submit` command. With sufficient iterations, the MSE should be less than 0.1.

### Trouble shooting
When you start the job, `spark-submit` may loops forever and the debug message may say `Initial job has not accepted any resources....`. There can be multiple reasons.

1. You have specified the wrong spark master. The best way is to copy and paste the `spark://your-hostname:your-port` string directly from the master UI page.
2. There is no workers in the master node. Make sure you have also connected the workers node to the master. Starting master alone is not sufficient, as it is just a resource manager.
3. By default, (which will be fix in near future), PredictionIO uses 8G of memory. This may exceed the amount of memory in your computer, and Spark by default uses (n-1) GB where n = GB RAM in your computer. You can change it to a lower value in [this line](https://github.com/PredictionIO/Imagine/blob/master/core/src/main/scala/workflow/EvaluationWorkflow.scala#L258).
