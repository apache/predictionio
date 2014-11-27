---
title: Contribute Documentation
---

## How to Write Documentation

You can help improve the PredictionIO documentation by submitting tutorials,
how-tos, fixing error, or adding missing information.

You can either open a [pull
request](https://help.github.com/articles/creating-a-pull-request/) on the
`livedoc` branch for instant update or the `develop` branch for the next
release.

If you are unsure of the documentation changes, please file an issue.


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

## Going Live

```
$ git push origin livedoc
```

[CircleCI](https://travis-ci.org/PredictionIO/PredictionIO) will take care of the rest!
