#!/bin/bash - 

set -e

if [[ $BUILD_TYPE == "Unit" ]]; then
  # Run stylecheck
  sbt scalastyle
  # Run all unit tests
  sbt test

else
  /testing/run_docker.sh $METADATA_REP $EVENTDATA_REP $MODELDATA_REP \
    / '/testing/simple_scenario/run_scenario.sh'
fi
