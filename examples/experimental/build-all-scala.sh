#!/usr/bin/env bash

set -e

for d in `find . -type d -maxdepth 1 | grep scala`
do
  echo "Building $d..."
  ( cd $d && ../../bin/pio build )
done
