#!/bin/bash -

set -e

if [[ $BUILD_TYPE == Unit ]]; then
  # Prepare pio environment variables
  set -a
  source conf/pio-env.sh.travis
  set +a

  # Run stylecheck
  sbt scalastyle
  # Run all unit tests
  sbt test

else
  REPO=`pwd`

  ./tests/run_docker.sh $METADATA_REP $EVENTDATA_REP $MODELDATA_REP \
    $REPO 'python3 /tests/pio_tests/tests.py'
fi
