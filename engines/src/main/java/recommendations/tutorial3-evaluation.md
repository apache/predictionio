# Tutorial 3 - Evaluation

Create Metrics.java

```java
// code
```

```
bin/pio-run io.prediction.engines.java.recommendations.Runner3
```

Test run with Movies-len 100k data set

Download ml-100k data set

```
curl -O http://files.grouplens.org/papers/ml-100k.zip
```

Unzip and copy to the testdata directory:

```
unzip ml-100k.zip
cp -r ml-100k engines/src/main/java/recommendations/testdata/
```

Run Runner3ML100k:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner3ML100k
```
