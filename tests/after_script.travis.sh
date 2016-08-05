#!/bin/bash -

set -e

if [[ $BUILD_TYPE == Unit ]]; then
  ./bin/travis/pio-stop-travis
fi
