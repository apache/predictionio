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

if [[ "$VOLUME_UID" == "1" || "$VOLUME_UID" == 'yes' ]]; then
  DIR_UID=`ls -lnd /home/jovyan/templates | awk '{print $3}'`
  if [ x"$DIR_UID" != "x" -a x"$DIR_UID" != "x0" ] ; then
    NB_UID=$DIR_UID
  fi
fi

if [ $(id -u) == 0 ] ; then
  if id jovyan &> /dev/null ; then
    echo "Set username to $NB_USER"
    usermod -d /home/$NB_USER -l $NB_USER jovyan
  fi

  if [[ "$CHOWN_HOME" == "1" || "$CHOWN_HOME" == 'yes' ]]; then
    echo "Change ownership of /home/$NB_USER to $NB_UID"
    chown -R $NB_UID /home/$NB_USER
  fi
  if [ ! -z "$CHOWN_EXTRA" ]; then
    for extra_dir in $(echo $CHOWN_EXTRA | tr ',' ' '); do
      chown -R $NB_UID $extra_dir
    done
  fi

  if [[ "$NB_USER" != "jovyan" ]]; then
    if [[ ! -e "/home/$NB_USER" ]]; then
      echo "Move home dir to /home/$NB_USER"
      mv /home/jovyan "/home/$NB_USER"
    fi
    if [[ "$PWD/" == "/home/jovyan/"* ]]; then
      newcwd="/home/$NB_USER/${PWD:13}"
      echo "Set CWD to $newcwd"
      cd "$newcwd"
    fi
  fi

  if [ "$NB_UID" != $(id -u $NB_USER) ] ; then
    echo "Set $NB_USER to uid:$NB_UID"
    usermod -u $NB_UID $NB_USER
  fi

  if [ "$NB_GID" != $(id -g $NB_USER) ] ; then
    echo "Add $NB_USER to gid:$NB_GID"
    groupadd -g $NB_GID -o ${NB_GROUP:-${NB_USER}}
    usermod -g $NB_GID -a -G $NB_GID,100 $NB_USER
  fi

  if [[ "$GRANT_SUDO" == "1" || "$GRANT_SUDO" == 'yes' ]]; then
    echo "Set sudo access to $NB_USER"
    echo "$NB_USER ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/notebook
  fi

  echo "Execute command as $NB_USER"
  exec su $NB_USER -c "env PATH=$PATH $*"

else
  echo "Execute command"
  exec $*

fi

