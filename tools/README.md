Running the Server
==================

1. sbt/sbt tools/run <run ID>
2. Go to http://localhost:8000
3. Send POST request to http://localhost:8000

Regression example:

```Shell
curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000
```
