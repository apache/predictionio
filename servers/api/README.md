How to build and run API server
===============================

## Prereq

1. Install [sbt](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html).
2. Install [play framework](http://www.playframework.com/download).

## Build and run
```
# From the repository root
sbt commons/publish output/publish

# Go to API server directory and run play framework
cd server/api/
play

# In play framework console
run
```
