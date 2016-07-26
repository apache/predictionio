---
title:  Submitting a Template to Template Gallery
---

## Template Guidelines

- Please give your template and GitHub repo a meaningful name (for example, My-MLlibKMeansClustering-Template).

- Please tag your repo for each released version. This is required by Template Gallery.

    For example, tag the release with v0.1.0:

    ```
    $ git tag -a v0.1.0 -m 'version 0.1.0'
    ```

- For clarity, the engine template directory structure should be:

    ```
    data/  # contains sample data or related files
    project/  # contains the necessary sbt files for build (e.g assembly.sbt)
    src/  # template source code
    .gitignore
    README.md
    build.sbt
    engine.json # one or more engine.json
    template.json
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


- If you have additional sample data, please also provide description and how to import them in README

- If you have multiple engine.json files, please provide description of them in README

- It's recommended to follow [Scala Style Guide](http://docs.scala-lang.org/style/)

## How to submit

- Fork repository
- Modify *docs/manual/source/gallery/templates.yaml* introducing a new template. The schema of the engine description is following:

```yml
- template:
    name: (Name of your template)
    repo: (Link to your repository)
    description: |-
      (Brief description of your template written in markdown syntax)
    tags: [ (One of [classification, regression, unsupervised, recommender, nlp, other]) ]
    type: (Parallel or Local)
    language: (Language)
    license: (License)
    status: (e.g. alpha, stable or requested (under development))
    pio_min_version: (Minimum version of PredictionIO to run your template)
```
- Submit your changes via pull-request
