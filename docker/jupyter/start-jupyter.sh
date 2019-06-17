#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

# store PIO environment to pio-env.sh
PIO_ENV_FILE=/etc/predictionio/pio-env.sh
env | grep ^PIO_ >> $PIO_ENV_FILE
if [ $(grep _MYSQL_ $PIO_ENV_FILE | wc -l) = 0 ] ; then
  sed -i "s/^MYSQL/#MYSQL/" $PIO_ENV_FILE
fi

# start event server
sh /usr/bin/pio_run &

export PYSPARK_PYTHON=$CONDA_DIR/bin/python
if [ x"$PYSPARK_DRIVER_PYTHON" = "x" ] ; then
  export PYSPARK_DRIVER_PYTHON=$CONDA_DIR/bin/jupyter
fi
if [ x"$PYSPARK_DRIVER_PYTHON_OPTS" = "x" ] ; then
  export PYSPARK_DRIVER_PYTHON_OPTS=notebook
fi

. /usr/local/bin/start.sh $PIO_HOME/bin/pio-shell --with-pyspark

