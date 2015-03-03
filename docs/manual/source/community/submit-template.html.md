---
title:  Submitting a Template to Template Gallery
---

## Template Guidelines

- Please follow [Scala Style Guide](http://docs.scala-lang.org/style/)

- Give your template and GitHub repo a meaningful name

- For clarity, the engine template directory structure should be:

  ```
  data/  # contains sample data or related files
  project/  # contains the necessary sbt files for build (e.g assembly.sbt)
  src/  # template source code
  .gitignore
  README.md
  build.sbt
  engine.json # one or more engine.json
  ```

- Try to keep the root directory clean. If you have additional script files or other files, please create new folders for them and provide description.

- Include a QuickStart of how to use the engine, including:
  1. Overview description of the template
  2. Events and Data required by the template
  3. Description of Query and PredictedResult
  4. Steps to import sample data
  5. Description of the sample data
  6. Steps to build, train and deploy the engine
  7. Steps to send sample query and expected output


- If you have additional sample data, please also provide description and how to import them.

- If you have multiple engine.json files, please provide description of them.

## How to submit

Go to http://templates.prediction.io/repositories/new and follow instructions.
