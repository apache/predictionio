---
title: Feature Selection and Dimensionality Reduction
---

The purpose of this guide is to give developers a comprehensive introduction to the topics in machine learning of feature selection and dimensionality reduction. This plays an important role in machine learning, and our goal is to give developers a base toolset and understanding for implementing feature selection and reduction. 

In machine learning, specifically in [supervised learning](http://en.wikipedia.org/wiki/Supervised_learning), the general problem at hand is to predict an outcome \\(y\\) from a numeric vector \\(\bf{x}\\), which we will, for the remainder of this guide, assume to be \\(p\\)-dimensional. The \\(p\\) components of \\(\bf{x}\\) are called **features**, and usually represent observed or recorded values such as a hospital patient's age, weight, height, sex, etc. At first glance, it is tempting to say that the more features, or information, we have, the better our predictions will be. However, there are subtle issues that begin to arise as \\(p\\), the number of features, increases. We briefly list some of the issues that arise as \\(p\\) grows in size:


- **Computation:** The time complexity of machine learning algorithms often times depends on the number of features used. That is, the more features one uses for prediction, the more time it takes to train a model.

- **Prediction Performance:** Often times there will be features that, when used in training, will actually decrease the predictive performance of a particular algorithm. 

- **Curse of Dimensionality:** It is harder to make inference and predictions in high dimensional spaces simply due to the fact that we need to sample a lot more observations. Think about it in this way, suppose that we sample 100 points lying on a flat solid square, and 100 points in a solid cube. The 100 points from the square will likely take up a larger proportion of its area, in comparison to the proportion of the cube's volume that the points sampled from it occupy. Hence we would need to sample more points from the cube in order to get better estimates of the different properties of the cube, such as height, length, and width. This is shown in the following figure:

| 100 Points Sampled From Unit Square                      | 100 Points Sampled From Unit Cube                    |
| -------------------------------------------------------- | ---------------------------------------------------- |
|                                                          |                                                      |
| ![Square Samples](/images/machinelearning/featureselection/square100.png) | ![Cube Samples](/images/machinelearning/featureselection/cube100.png) |
|                                                          |                                                      |

Feature selection is the process of selecting a smaller subset from the originally given \\(p\\) features. Dimensionality reduction refers to the process of applying a transformation on your feature vector that results in a new numeric vector with less features. We will give examples of some techniques that can serve as quick  solutions to increase the performance of the machine learning algorithms that are powering your applications. We will be including code implementations using the ML library in Spark 1.4.0.

## Guiding Data Example

As a guiding example, we will be looking at a simple [data set](https://archive.ics.uci.edu/ml/datasets/Concrete+Compressive+Strength) from the UCI repository. The response variable here is concrete compressive strength which is important in Civil Engineering applications. To predict this, the data set gives the Age and a set of Ingredient amounts as feature variables.



The following class is used to store the data as a Spark DataFrame. 

**Note:** We also implemented some methods for nicely printing output that are excluded here for brevity. 

```scala
package FeatureSelection

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.{DataFrame, SQLContext}
import scala.math.abs

class ConcreteData(sc : SparkContext) {

  // SQLContext for DataFrame creation.
  val sql : SQLContext = new SQLContext(sc)
  import sql.implicits._

  // Feature variable names.
  val varNames : Array[String] = Array(
    "Cement",
    "Blast Furnace Slag",
    "Fly Ash",
    "Water",
    "Superplasticizer",
    "Coarse Aggregate",
    "Fine Aggregate",
    "Age"
  )

  // Import data into Spark DataFrame.
  val data : DataFrame = sc.textFile(
    "./concrete_data.txt"
  ).map(
    e => {
      // Split by comma delimiter and change to numeric features.
      val array = e.split(" ,").map(_.toDouble)
      LabeledPoint(
        array.takeRight(1)(0), // Get concrete compressive strengh.
        Vectors.dense(array.take(array.size - 1)) // Get features.
      )
    }
  ).toDF

  ...
}
```


## Linear Shrinkage Methods for Feature Selection

In this section we will give three different methods for selecting a subset of features. All three methods are derived by placing additional constraints on the estimates obtained from [least squares](http://en.wikipedia.org/wiki/Least_squares) linear regression. 

In least squares, the linear regression coefficient vector estimate \\(\hat{\beta}\\) is chosen to minimize a measure of predictive performance which can be conveniently written as a "nice" function of \\(\beta\\), say \\(L(\beta)\\), which only outputs non-negative numeric values. That is, the estimate \\(\hat{\beta}\\) satisfies the expression:

$$
\underset{\beta}{\text{argmin}} \ L(\beta).
$$ 

The argmin function is just notation for expressing the set of linear regression coefficient vectors that minimize the predictive performance measure \\(L\\). Also, in order to provide  some more context, you should note that \\(\hat{\beta}\\) is the vector obtained from a `LinearRegressionModel` object in Spark ML using the class member `weights`.

Each of the following three subset selection methods we will discuss are rooted in the above minimization criterion. 

### Ridge Regression for Subset Selection

Now, \\(\beta\\) is also a vector with \\(p\\) components \\(\beta_1, ..., \beta_p\\). Ridge regression looks for a coefficient regression vector estimate that satisfies the minimization criterion

$$
\underset{\beta}{\text{argmin}} \ L(\beta) + \lambda \cdot \left(\beta\_1\^2 + \cdots + \beta\_p\^2 \right).
$$

Intuitively, adding the extra term in the minimization criterion forces the linear regression coefficients to take values closer to 0. Here, \\(\lambda\\) is a number greater than or equal to zero that determines how much the linear coefficients are shrunk towards zero. Increasing the value of \\(\lambda\\) will cause the coefficients to shrink closer to zero.

Now, to select a subset of features, we first decide on a threshold \\(t\\) which can take on values greater than or equal to 0. We will select those features corresponding to regression coefficients \\(\beta\_i\\) whose distance from 0 is greater than \\(t \ \left(\text{i.e.} \ \left|\beta\_i\right| > t\right)\\). The following code sample shows how to implement this using Spark ML:


```scala
import FeatureSelection.ConcreteData
import org.apache.spark.SparkContext
val sc : SparkContext // Existing SparkContext.

import org.apache.spark.ml.regression.LinearRegression

val concreteData = new ConcreteData(sc)


//// Ridge Regression Based Feature Selection


// Elastic net parameter = 0 means using L2 regularization a.k.a. Ridge Regression.
val ridge = new LinearRegression().setElasticNetParam(0)


// Get coefficients for three different regularization parameter settings.
val ridgeCoeffsOne = ridge.setRegParam(1).fit(concreteData.data).weights.toArray
val ridgeCoeffsTwo = ridge.setRegParam(3).fit(concreteData.data).weights.toArray
``` 



In the above code block, the `setRegParam` is used to set the \\(\lambda\\) value we previously discussed. Don't worry about the method `setElasticNetParam` yet, we will discuss this soon. The last two lines of code extract the coefficient estimates produced by ridge regression with \\(\lambda = 1, 3\\), respectively. The code below is used to print out the selected features at a fixed threshold \\(t = 0.05\\) (the printed output is shown as comments):

```scala
concreteData.printFeatures(ridgeCoeffsOne, 0.05)
// Cutoff threshold: 0.05
// These are the features selected by procedure:
//
// Feature name: Cement, Coefficient: 0.08021334588574032
// Feature name: Blast Furnace Slag, Coefficient: 0.057448612590709165
// Feature name: Water, Coefficient: -0.2363734779579686
// Feature name: Superplasticizer, Coefficient: 0.3789971191686809
// Feature name: Age, Coefficient: 0.10357527665180728
//
// These are the features that were left out:
//
// Feature name: Fly Ash, Coefficient: 0.03152471394976905
// Feature name: Coarse Aggregate, Coefficient: -0.011900573600572806
// Feature name: Fine Aggregate, Coefficient: -0.01898596015615296


concreteData.printFeatures(ridgeCoeffsTwo, 0.05)
// Cutoff threshold: 0.05
// These are the features selected by procedure:
//
// Feature name: Cement, Coefficient: 0.06289392408886504
// Feature name: Water, Coefficient: -0.21408844646878866
// Feature name: Superplasticizer, Coefficient: 0.4754004409674183
// Feature name: Age, Coefficient: 0.08959421410735308
//
// These are the features that were left out:
//
// Feature name: Blast Furnace Slag, Coefficient: 0.0384176284232736
// Feature name: Fly Ash, Coefficient: 0.009253466362541134
// Feature name: Coarse Aggregate, Coefficient: -0.01663407049361568
// Feature name: Fine Aggregate, Coefficient: -0.02625467356150417
```

As you can see above, increasing the regularization parameter \\(\lambda\\) causes the regression coefficients to shrink closer to zero. In fact, we see that ridge regression with parameter \\(\lambda = 3\\) (corresponds to `ridgeCoeffsTwo`) discards four features, while the other only discards three. Of course, another variable that will affect the number of features selected is the threshold - as \\(t\\) becomes larger, the number of features selected becomes smaller. 

We will now show the method `printFeatures` defined in the class `ConcreteData` to demonstrate how the threshold selection is implemented:

```scala
def printFeatures (coeffs : Array[Double], threshold : Double): Unit = {
  println
  println("Cutoff threshold: " + threshold)
  println("These are the features selected by procedure:")
  println

  (0 until coeffs.size)
  // Only select features whose distance from 0 is greater than the set threshold.
  .filter(k => abs(coeffs(k)) > threshold)
  // Collect the variable names and coefficient values.
  .map(k => (varNames(k), coeffs(k)))
  .foreach(e => println("Feature name: " + e._1 + ", Coefficient: " + e._2))

  println
  println("These are the features that were left out:")
  println

  (0 until coeffs.size)
  // Discard features whose distance from 0 is less than or equal to the set threshold.
  .filter(k => abs(coeffs(k)) <= threshold)
  // Collect the variable names and coefficient values.
  .map(k => (varNames(k), coeffs(k)))
  .foreach(e => println("Feature name: " + e._1 + ", Coefficient: " + e._2))
 }
```

### Lasso Regression for Subset Selection

The next subset selection technique we discuss is Lasso regression. This is a result from a minimization criterion that is very similar to that of ridge regression, namely,

$$
\underset{\beta}{\text{argmin}} \ L(\beta) + \lambda \cdot \left(\left|\beta\_1\right| + \cdots + \left|\beta\_p\right| \right).
$$

As you can see, it replaces \\(\beta\_i\^2\\) with \\(\left|\beta\_i\right|\\). This achieves the same effect as ridge regression, except that the coefficient shrinkage is much more extreme. In fact, using ridge regression will usually not set any coefficients exactly equal to 0, which is not the case with Lasso regression. The effect of the regularization parameter \\(\lambda\\) is the same as with ridge, higher values result in coefficients closer to zero. The following code shows how to implement Lasso using Spark ML:

```scala
import FeatureSelection.ConcreteData
import org.apache.spark.SparkContext
val sc : SparkContext // Existing SparkContext.

import org.apache.spark.ml.regression.LinearRegression

val concreteData = new ConcreteData(sc)


//// Lasso Regression Based Feature Selection


// Elastic net parameter = 1 means using L1 regularization a.k.a. Lasso Regression.
val lasso = new LinearRegression().setElasticNetParam(1)


// Get coefficients for three different regularization parameter settings.
val lassoCoeffsOne = lasso.setRegParam(1).fit(concreteData.data).weights.toArray
val lassoCoeffsTwo = lasso.setRegParam(3).fit(concreteData.data).weights.toArray
```

Again, the last two lines obtain the Lasso regression coefficients with regularization parameter \\(\lambda = 1, 3\\), respectively. This time we will give a printed output of the features selected at the threshold \\(t = 0\\):

```scala
concreteData.printFeatures(lassoCoeffsOne, 0.0)
// Cutoff threshold: 0.0
// These are the features selected by procedure:
//
// Feature name: Cement, Coefficient: 0.06824292686169534
// Feature name: Blast Furnace Slag, Coefficient: 0.04053311287270642
// Feature name: Water, Coefficient: -0.1483657015631392
// Feature name: Superplasticizer, Coefficient: 0.5700142957197362
// Feature name: Fine Aggregate, Coefficient: -0.006798884030660536
// Feature name: Age, Coefficient: 0.08728925843613869
//
// These are the features that were left out:
//
// Feature name: Fly Ash, Coefficient: 0.0
// Feature name: Coarse Aggregate, Coefficient: 0.0

concreteData.printFeatures(lassoCoeffsTwo, 0.0)
// Cutoff threshold: 0.0
// These are the features selected by procedure:
//
// Feature name: Cement, Coefficient: 0.047517124591797244
// Feature name: Blast Furnace Slag, Coefficient: 0.007510319134297981
// Feature name: Water, Coefficient: -0.015824951837943062
// Feature name: Superplasticizer, Coefficient: 0.4918144399832059
// Feature name: Age, Coefficient: 0.04389535227168315
//
// These are the features that were left out:
//
// Feature name: Fly Ash, Coefficient: 0.0
// Feature name: Coarse Aggregate, Coefficient: 0.0
// Feature name: Fine Aggregate, Coefficient: 0.0
```

We again see the same pattern with respect to \\(\lambda\\). At \\(\lambda = 1\\) (corresponds to `lassoCoeffsOne`), we see that only two features are discarded. At \\(\lambda = 3\\), we have three features being discarded. However, we see here that the Lasso regression actually sets some of the feature regression coefficients to 0! This exemplifies the point made above: the coefficient shrinkage with Lasso is more extreme than with ridge.

### Elastic Net Regression for Subset Selection

The third and last feature selection method we include in this guide is a compromise between Lasso and ridge regression: elastic net regression. We note that elastic net regression as presented below is not implemented in Spark versions lower than 1.4.0.

The minimization criterion used for elastic net is of the form

$$
\underset{\beta}{\text{argmin}} \ L(\beta) + \lambda \cdot \left[\alpha \cdot \left(\left|\beta\_1\right| + \cdots + \left|\beta\_p\right| \right) + (1 - \alpha) \cdot \left(\beta\_1\^2 + \cdots + \beta\_p\^2 \right)\right].
$$

Note that here we introduce a new parameter, \\(\alpha\\). This is a variable that satisfies \\(0 \leq \alpha \leq 1\\), and is precisely the parameter that you set using the method `setElasticNetParam`. In fact, pay attention to the following lines we used to implement the ridge and Lasso regression models for subset selection:

Ridge Regression:

```scala
// Elastic net parameter = 0 means using L2 regularization a.k.a. Ridge Regression.
val ridge = new LinearRegression().setElasticNetParam(0)
```

Lasso Regression:

```scala
// Elastic net parameter = 1 means using L1 regularization a.k.a. Lasso Regression.
val lasso = new LinearRegression().setElasticNetParam(1)
```

As you can see, the extreme values that \\(\alpha\\) can take, 0 and 1, correspond to ridge and Lasso regression, respectively. This should yield the intuition that elastic net regression is a compromise between (or rather, a generalization of) Lasso and ridge regression. 

It follows that the Spark ML implementation for elastic net is the same as the Spark ML implementation of Lasso and ridge regression, except you have the added flexibility of selecting the parameter \\(\alpha\\) which is set using the `setElasticNetParam` method seen above. When you are going through the process of [parameter tuning](/evaluation/paramtuning/) is that when using these linear shrinkage methods for subset selection you will have three parameters for tuning: \\(\lambda\\), your regularization parameter; \\(\alpha\\), your elastic net parameter; and \\(t\\), your threshold parameter. 

## Linear Algebra Methods for Dimensionality Reduction 

In this section we will discuss two different methods for reducing the number of features by applying a transformation on our data feature vectors. These are both methods that are intimately related to the [eigenvectors](https://en.wikipedia.org/wiki/Eigenvalues_and_eigenvectors) of the matrix obtained by using the feature vectors in your training data as rows. Recall that in our guiding data example, we are given 1,030 observations associated to feature vectors with 8 different ingredient/age components. 

We can naturally collect our observations into a \\(1030 \times 8\\) matrix \\(\bf{X}\\) by letting each of its rows be one of the 1,030 feature vectors given in the data set. Our specific problem will be to reduce the number of features to, say 5. 

Now that we have represented our data as a matrix, we have set the stage to use tools from linear algebra. The main tool we will employ is the [Singular Value Decomposition](https://spark.apache.org/docs/latest/mllib-dimensionality-reduction.html#singular-value-decomposition-svd) (SVD) of a matrix. In doing this, what we will essentially be doing is viewing our feature vectors using a different and more "convenient" frame of reference, and then removing a part of the space that yields the smallest amount of information. 

### Principal Component Analysis for Feature Reduction

Principal component analysis (PCA) begins by looking at the covariance matrix of our data which we call \\(\text{Cov}({\bf X})\\). This matrix essentially yields information regarding how the 8 feature components interact, or vary, and in statistics is something that has to be estimated from the data. We then use the SVD of \\(\text{Cov}({\bf X})\\) to extract the space that explains the majority of the variation contained in the data. 

It turns out that if, for each of the eight columns in our data matrix, we subtract each of the column entries by the average of all the entries in the column and call the resulting matrix \\({\bf X}\_c\\), then you can write the usual statistical estimate of \\(\text{Cov}({\bf X})\\) as the matrix \\({\bf X}\_c\^T{\bf X}\_c\\). This little trick, called centering the matrix \\(\bf X\\),  reduces PCA to performing SVD on the centered data matrix \\({\bf X}\_c\\). In particular, you extract the \\(\bf V\\) component of its [decomposition](https://spark.apache.org/docs/latest/mllib-dimensionality-reduction.html#singular-value-decomposition-svd):

$$
{\bf X}\_c = {\bf U}\Sigma{\bf V}\^T.
$$

The matrix \\(\bf V\\) will be of size \\([\text{actual number of features}] \times [\text{desired number of features}] = 8 \times 5\\). The desired matrix with a reduced number of features is then given by the \\(1030 \times 5\\) matrix \\({\bf X}_c{\bf V}\\). The columns of \\(\bf V\\) are called the principal components of \\(\bf{X}\\), and essentially are the building blocks of our lower-dimensional representation of the data.

The following code demonstrates how to implement PCA step-by-step using Spark ML:

```scala
import FeatureSelection.ConcreteData
import org.apache.spark.SparkContext


val sc : SparkContext // Existing SparkContext.

import org.apache.spark.ml.feature.StandardScaler
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Vector

val concreteData = new ConcreteData(sc)


// Principal Components Analysis.

// The intuition here is that you are taking your features and
// expressing them as linear combinations of a set of vectors that explain
// the largest proportion of variance in your original set of features.

// This will be used to obtain the centered data matrix X_c.
val standardScaler = new StandardScaler().setInputCol("features").setOutputCol("centered")
val standardScalerModel = standardScaler.setWithMean(true).fit(concreteData.data)
val standardData = standardScalerModel.transform(concreteData.data)


// Extract the centered data matrix from the given DataFrame.
val featureMatrix = new RowMatrix(standardData.select("centered").rdd.map(
  e => e.getAs[Vector](0) // Fetch rows as vectors.
))

// Multiply by principal components to obtain matrix with original features
// written as a linear combination of high variance eigenvectors.
//
// This is the lower-dimensional representation of the original data matrix.
val reducedMatrix = featureMatrix.multiply(
  featureMatrix.computeSVD(4).V // Extract V from SVD decomposition of centered matrix.
)
```




