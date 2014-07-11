Running the Server
==================

Assuming you have finished an evaluation run successfully and you have a run ID available.
The run ID can be found after a successful run in the console.

```Shell
sbt/sbt deploy/assembly
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
spark-submit --jars engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar,engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar --class io.prediction.deploy.RunServer deploy/target/scala-2.10/deploy-assembly-0.8.0-SNAPSHOT.jar RunID
```

After that, go to http://localhost:8000 and send a POST request.

Regression example:

```Shell
curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000

curl -H "Content-Type: application/json" -d '[-0.8600615539670898, -1.0084357652346345, -1.3088407119560064, -1.9340485539299312, -0.6246990990796732, -2.325746651211032, -0.28429904752434976, -0.1272785164794058, -1.3787859877532718, -0.24374419289538318]' http://localhost:8000
```
