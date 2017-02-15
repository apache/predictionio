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

FROM ubuntu:xenial

ENV SPARK_VERSION 1.4.0
ENV ELASTICSEARCH_VERSION 1.4.4
ENV HBASE_VERSION 1.0.0

RUN apt-get update && apt-get install -y \
    wget curl \
    python-pip \
    python3-pip \
    postgresql-client \
    openjdk-8-jdk \
    openssh-client openssh-server \
    git

RUN pip install predictionio
RUN pip3 install --upgrade \
    pip \
    xmlrunner \
    requests \
    urllib3

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/jre

ADD docker-files/spark-${SPARK_VERSION}-bin-hadoop2.6.tgz /vendors
ENV SPARK_HOME /vendors/spark-${SPARK_VERSION}-bin-hadoop2.6

ENV ELASTICSEARCH_HOME /vendors/elasticsearch-${ELASTICSEARCH_VERSION}

ENV HBASE_HOME /vendors/hbase-${HBASE_VERSION}

COPY docker-files/postgresql-9.4-1204.jdbc41.jar /drivers

ENV PIO_HOME /PredictionIO
ENV PATH ${PIO_HOME}/bin/:${PATH}
COPY dist ${PIO_HOME}

COPY docker-files/init.sh init.sh
COPY docker-files/env-conf/spark-env.sh ${SPARK_HOME}/conf/spark-env.sh
COPY docker-files/env-conf/hbase-site.xml ${HBASE_HOME}/conf/hbase-site.xml
COPY docker-files/env-conf/pio-env.sh /pio-env.sh
COPY docker-files/wait-for-postgres.sh /wait-for-postgres.sh
COPY docker-files/pgpass /root/.pgpass

# Default repositories setup
ENV PIO_STORAGE_REPOSITORIES_METADATA_SOURCE PGSQL
ENV PIO_STORAGE_REPOSITORIES_EVENTDATA_SOURCE PGSQL
ENV PIO_STORAGE_REPOSITORIES_MODELDATA_SOURCE PGSQL

# JVM settings
ENV JVM_OPTS '-Dfile.encoding=UTF8 -Xms2048M -Xmx2048M -Xss8M -XX:MaxPermSize=512M -XX:ReservedCodeCacheSize=256M'

# Expose relevant ports
# pio engine
EXPOSE 8000
# eventserver
EXPOSE 7070

ENTRYPOINT ["/init.sh"]
CMD 'bash'
