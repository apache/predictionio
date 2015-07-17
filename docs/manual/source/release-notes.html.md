---
title: Release Notes and News
---

INFO: For upgrade instructions please refer to [this page](/resources/upgrade/).

### What's New

0.9.4 Release | July 16th, 2015 | [Release Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13700)

- Support event permissions with different access keys at the Event Server interface

- Support detection of 3rd party Apache Spark distributions

- Support running `pio eval` without `engine.json`

- Fix an issue where `--verbose` is not handled properly by `pio train`

0.9.3 Release | May 20th, 2015 | [Release Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13600)

- Add support of developing prediction engines in Java

- Add support of PostgreSQL and MySQL

- Spark 1.3.1 compatibility fix

- Creation of specific app access keys

- Prevent a case where `pio build` accidentally removes PredictionIO core library

0.9.2 Release | April 14th, 2015 | [Release Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13500)

- Channels in the Event Server

- Spark 1.3+ support (upgrade to Spark 1.3+ required)

- [Webhook Connector](http://docs.prediction.io/community/contribute-webhook/) support

- Engine and Event Servers now by default bind to 0.0.0.0

- Many documentation improvements

0.9.1 Release | March 17th, 2015 | [Releaes Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13401)

- Improved `pio-start-all`

- Fixed a bug where `pio build` failed to set PredictionIO dependency version for engine templates

0.9.0 Release | March 4th, 2015 | [Release Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13400)

- [E-Commerce Recommendation Template](http://templates.prediction.io/PredictionIO/template-scala-parallel-ecommercerecommendation) which includes 1) out-of-stock items support 2) new user recommendation 3) unseen items only

- [Complementary Purchase Template](http://templates.prediction.io/PredictionIO/template-scala-parallel-complementarypurchase) for shopping cart recommendation

- [Lead Scoring Template](http://templates.prediction.io/PredictionIO/template-scala-parallel-leadscoring) predicts the probability of an user will convert in the current session

- `pio-start-all`, `pio-stop-all` commands to start and stop all PredictionIO related services

0.8.6 | Feb 10th, 2015 | [Release Notes](https://predictionio.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=13300)

- New engine template - [Product Ranking](/templates/productranking/quickstart/) for personalized product listing

- [CloudFormation deployment](/system/deploy-cloudformation/) available
