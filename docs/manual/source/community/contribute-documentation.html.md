---
title: Contribute Documentation
---

## How to Write Documentation

You can help improve the PredictionIO documentation by submitting tutorials,
writing how-tos, fixing errors, and adding missing information. You can edit any page
live on [GitHub](https://github.com/PredictionIO/PredictionIO) by clicking the pencil icon on any page or open a
[pull request](https://help.github.com/articles/creating-a-pull-request/).


## Branching

Use the `livedoc` branch if you want to update the current documentation.

<img src="https://travis-ci.org/PredictionIO/PredictionIO.svg?branch=livedoc" alt="Build Status" class="static" />

Use the `develop` branch if you want to write documentation for the next version.

<img src="https://travis-ci.org/PredictionIO/PredictionIO.svg?branch=develop" alt="Build Status" class="static" />


## Installing Locally

PredictionIO documentation uses [Middleman](http://middlemanapp.com/). You can install it with the following commands:

```bash
$ cd docs/manual
$ gem install bundler
$ bundle install
```

## Starting the Server

Start the server with:

```
$ bundle exec middleman server
```

This will start the local web server at [http://localhost:4567](http://localhost:4567/).

## Building the Site

Build the site with:

```
$ bundle exec middleman build
```

## Styleguide

Please follow this styleguide for any documentation contributions.

### Links

Internal links:

* Should start with / (relative to root).
* Should end with / (S3 requirement).
* Should **not** end with .html.

Following these rules helps keep everything consistent and allows our version parser to correctly version links.
Middleman is configured for directory indexes. Linking to a file in `sources/samples/index.html` should be done with
`[Title](/sample/)`.

```md
[Good](/path/to/page/)

[Bad](../page) Not relative to root!
[Bad](page.html) Do not use the .html extension!
[Bad](/path/to/page) Does not end with a /.

```

## Going Live

Pushing to the `livedoc` branch will update [docs.prediction.io](http://docs.prediction.io/) in about 5 minutes.

You can check the progress of each build on [Travis CI](https://travis-ci.org/PredictionIO/PredictionIO).

```
$ git push origin livedoc
```

## Checking the Site

WARNING: The check rake task is still in **beta** however it is extremely useful for catching accidental errors.

```bash
$ bundle exec middleman build
$ bundle exec rake check
```

The `rake check` task parses each HTML page in the `build` folder and checks it for common errors including 404s.
