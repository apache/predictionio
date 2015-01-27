---
title:  Contribute Code
---

Thank you for your interest in contributing to PredictionIO. Our mission is to
enable developers to build scalable machine learning applications easily. Here is how you can help with the project development. If you have questions at anytime, please free to post on the [Developer Fourm](https://groups.google.com/forum/#!forum/predictionio-dev) or email us at support@prediction.io.

## Areas in Need of Help

We accept contributions of all kinds at any time. We are compiling this list to
show features that are highly sought after by the community.

- Tests and CI
- Engine template, tutorials, and samples
- Client SDKs
- Building engines in Java (updating the Java controller API)
- Code clean up and refactoring
- Code and data pipeline optimization
- Developer experience (UX) improvement

## How to Report an Issue

If you wish to report an issue you found,  you can do so on [GitHub Issues](https://github.com/PredictionIO/PredictionIO/issues)
or [PredictionIO JIRA](https://predictionio.atlassian.net).

## How to Help Resolve Existing Issues

In general, bug fixes should be done the same way as new features, but critical
bug fixes will follow a different path.

#### Critical Bug Fixes Only

1. File an issue against the issue tracker if there isn't one already.
2. Create a new hotfix branch described by the *git flow* methodology, and write
   the fix in that branch.
3. Verify the patch and issue a pull request to the main repository.
4. Once merged to the main repository, critical bug fixes will be merged to the
   "master" branch and new binary will be built and distributed.
   

## How to Add / Propose a New Feature

1. To propose a new feature, simply post your proposal to [PredictionIO
   Development Google Group]
   (https://groups.google.com/forum/#!forum/predictionio-dev) or email us directly at support@prediction.io.
2. Discuss with the community and the core development team on what needs to be
   done, and lay down concrete plans on deliverables.
3. Once solid plans are made, start creating tickets in the [issue tracker]
   (https://predictionio.atlassian.net/secure/RapidBoard.jspa?rapidView=1).
4. Work side by side with other developers using PredictionIO Development Google
   Group as primary mode of communication. You never know if someone else has a
   better idea. ;)
  

## How to Issue a Pull Request

When you have finished your code, you can [create a pull
request](https://help.github.com/articles/creating-a-pull-request/) against the
**develop** branch. You also need to complete the [Contributor Agreement](http://prediction.io/cla). We cannot accept your PR without the agreement.

- Make sure the title and description are clear and concise. For more details on
  writing a good commit message, check out [this
  guide](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).
- If the change is visual, make sure to include a screenshot or GIF.
- If the PR closes an issue, make sure to put *Closes #X* at the end of the
  description on a new line.
- Make sure it is being opened into the right branch.
- Make sure it has been rebased on top of that branch.

NOTE: When it is close to a release, and if there are major development ongoing, a
release branch will be forked from the develop branch to stabilize the code for
binary release. Please refer to the *git flow* methodology page for more
information.

## Getting Started

PredictionIO relies heavily on the [git flow methodology](
http://nvie.com/posts/a-successful-git-branching-model/). Please make sure you
read and understand it before you start your development. By default, cloning
PredictionIO will put you in the *develop* branch, which in most cases is where
all the latest development go to.

### Create a Clone of PredictionIO’s Repository

1. Start by creating a GitHub account if you do not already have one.
2. Go to [PredictionIO’s
   repository](https://github.com/PredictionIO/PredictionIO) and fork it to your
   own account.
3. Clone your fork to your local machine.

If you need additional help, please refer to
https://help.github.com/articles/fork-a-repo/.

### Building PredictionIO from Source

After the previous section, you should have a copy of PredictionIO in your local
machine ready to be built.

1. Make sure you are on the *develop* branch. You can double check by `git
   status` or simply `git checkout develop`.
2. At the root of the repository, do `./make-distribution.sh` to build
   PredictionIO.

### Setting Up the Environment

PredictionIO relies on 3rd party software to perform its tasks. To set them up,
simply follow this [documentation](
http://docs.prediction.io/install/install-sourcecode/#installing-dependencies).

### Start Hacking

You should have a PredictionIO development environment by now. Happy hacking!

## Anatomy of PredictionIO’s Code Tree

The following describes each directory’s purpose.

### bin

Shell scripts and any relevant components to go into the binary distribution.
Utility shell scripts can also be included here.

### conf

Configuration files that are used by both a source tree and binary distribution.

### core

Core PredictionIO code that provides the DASE controller API, core data
structures, and workflow creation and management code.

### data

PredictionIO Event Server, and backend-agnostic storage layer for event store
and metadata store.

### docs

Source code for docs.prediction.io site, and any other documentation support
files.

### engines

Obsolete built-in engines. To be removed.

### examples

Complete code examples showing PredictionIO’s application.

### sbt

Embedded SBT (Simple Build Tool) launcher.

### templates

Starting point of building your custom engine.

### tools

Tools for running PredictionIO. Contains primarily the CLI (command-line
interface) and its supporting code, and the experimental evaluation dashboard.
