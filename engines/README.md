# How to run an engine (evaluation)

There are three main components:
- PredictionIO core library jar
- External dependencies jar
- Engine jar (for engine builders, this is where you update your code)

The first two jar only requires one time compile, while the engine jar needs to be re-compiled everytime you change the code.

## Compiling external dependencies
```
sbt/sbt "project engines" assemblyPackageDependency
```
