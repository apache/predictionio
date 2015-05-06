---
title: Using Another Data Store
---

PredictionIO has a thin storage layer to abstract meta data, event data, and
model data access. The layer defines a set of standard interfaces to support
multiple data store backends. PredictionIO users can configure the backend of
choice through configuration files or environmental variables. Engine developers
need not worry about the actual underlying storage architecture. Advanced
developers can implement their own backend driver as an external library.


## Concepts

In this section, we will visit some storage layer concepts that are common to
users, engine developers, and advanced developers.


### Repositories

Repository is the highest level of data access abstraction and is where all
engines and PredictionIO itself access data with.

The storage layer currently defines three mandatory data repositories: *meta
data*, *event data*, and *model data*. Each repository has its own set of data
access interfaces.

*Meta data* is used by PredictionIO to store engine training and evaluation
information. Commands like `pio build`, `pio train`, `pio deploy`, and `pio
eval` all access meta data.

*Event data* is used by the Event Server to collect events, and by engines to
source data.

*Model data* is used by PredictionIO for automatic persistence of trained
models.

To configure these repositories, look for the following configuration variables
in `conf/pio-env.sh`.

```shell
PIO_STORAGE_REPOSITORIES_METADATA_NAME=pio_metadata
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=PGSQL

PIO_STORAGE_REPOSITORIES_EVENTDATA_NAME=pio_eventdata
PIO_STORAGE_REPOSITORIES_EVENTDATA_SOURCE=HBASE

PIO_STORAGE_REPOSITORIES_MODELDATA_NAME=pio_modeldata
PIO_STORAGE_REPOSITORIES_MODELDATA_SOURCE=HDFS
```

Notice that repository names (*METADATA*, *EVENTDATA*, and *MODELDATA*) are
fixed.

The configuration variable with the *NAME* suffix controls the namespace used by
the *source*.

The configuration variable with the *SOURCE* suffix points to the actual
*source* that will back this repository. *Source* will be explained below.


### Sources

Sources are actual data store backends that provide data access. A source is an
implementation of the set of data access interfaces defined by repositories.

PredictionIO comes with the following sources:

-   Meta Data
    -   JDBC (tested on MySQL and PostgreSQL)
    -   Elasticsearch
-   Event Data
    -   JDBC (tested on MySQL and PostgreSQL)
    -   Apache HBase
-   Model Data
    -   JDBC (tested on MySQL and PostgreSQL)
    -   Local file system
    -   HDFS

Each repository can be configured to use different sources as shown before.

Each source has its own set of configuration parameters. Configuration variables
are documented in sections below. The following is an example source
configuration:

```shell
PIO_STORAGE_SOURCES_PGSQL_TYPE=jdbc
PIO_STORAGE_SOURCES_PGSQL_URL=jdbc:postgresql:predictionio
PIO_STORAGE_SOURCES_PGSQL_USERNAME=pio
PIO_STORAGE_SOURCES_PGSQL_PASSWORD=pio
```


## Data Store Configuration

Data store configuration is done by settings environmental variables. If you set
them inside `conf/pio-env.sh`, they will be automatically available whenever you
perform a `pio` command, e.g. `pio train`.

Notice that all variables are prefixed by `PIO_STORAGE_`.


### Repositories

Variable Format: `PIO_STORAGE_REPOSITORIES_<REPO>_<KEY>`

Configuration variables of repositories are prefixed by
`PIO_STORAGE_REPOSITORIES_`, followed by the repository name (e.g. `METADATA`),
and either `NAME` or `SOURCE`.

Consider the following example:

```shell
PIO_STORAGE_REPOSITORIES_METADATA_NAME=pio_metadata
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=PGSQL
```

The above configures PredictionIO to look for a source configured with the name
`PGSQL`, and use `pio_metadata` as the namespace within such source. There is no
restriction on namespace usage by the source, so behavior may vary. As an
example, the official JDBC source uses the namespace as database table prefix.


### Sources

Variable Format: `PIO_STORAGE_SOURCES_<NAME>_<KEY>`

Configuration variables of sources are prefixed by
`PIO_STORAGE_SOURCES_`, followed by the source name of choice (e.g. `PGSQL`,
`MYSQL`, `HBASE`, etc), and a configuration `KEY`.

INFO: The `TYPE` configuration key is mandatory. It is used by PredictionIO to
determine the actual driver type to load.

Depending on what the source `TYPE` is, different configuration keys are
required.


#### JDBC

Variable Format: `PIO_STORAGE_SOURCES_[NAME]_TYPE=jdbc`

Supported Repositories: **meta**, **event**, **model**

Tested on: MySQL 5.1+, PostgreSQL 9.1+

When `TYPE` is set to `jdbc`, the following configuration keys are supported.

-   URL (mandatory)

    The value must be a valid JDBC URL that points to a database, e.g.
    `PIO_STORAGE_SOURCES_PGSQL_URL=jdbc:postgresql:predictionio`

-   USERNAME (mandatory)

    The value must be a valid, non-empty username for the JDBC connection, e.g.
    `PIO_STORAGE_SOURCES_PGSQL_USERNAME=pio_user`

-   PASSWORD (mandatory)

    The value must be a valid, non-empty password for the JDBC connection, e.g.
    `PIO_STORAGE_SOURCES_PGSQL_PASSWORD=pio_user_password`

-   PARTITIONS (optional, default to 4)

    This value is used by Apache Spark to determine the number of partitions to
    use when it reads from the JDBC connection, e.g.
    `PIO_STORAGE_SOURCES_PGSQL_PARTITIONS=4`


#### Apache HBase

Variable Format: `PIO_STORAGE_SOURCES_[NAME]_TYPE=elasticsearch`

Supported Repositories: **event**

Tested on: Apache HBase 0.98.5+, 1.0.0+

When `TYPE` is set to `hbase`, no other configuration keys are required. Other
client side HBase configuration must be done through `hbase-site.xml` pointed
by the `HBASE_CONF_DIR` configuration variable.


#### Elasticsearch

Variable Format: `PIO_STORAGE_SOURCES_[NAME]_TYPE=elasticsearch`

Supported Repositories: **meta**

When `TYPE` is set to `elasticsearch`, the following configuration keys are
supported.

-   HOSTS (mandatory)

    Comma-separated list of hostnames, e.g.
    `PIO_STORAGE_SOURCES_ES_HOSTS=es1,es2,es3`

-   PORTS (mandatory)

    Comma-separated list of ports that corresponds to `HOSTS`, e.g.
    `PIO_STORAGE_SOURCES_ES_PORTS=9200,9200,9222`

-   CLUSTERNAME (optional, default to `elasticsearch`)

    Elasticsearch cluster name, e.g.
    `PIO_STORAGE_SOURCES_ES_CLUSTERNAME=myescluster`

INFO: Other advanced Elasticsearch parameters can be set by pointing
`ES_CONF_DIR` configuration variable to the location of `elasticsearch.yml`.


#### Local File System

Variable Format: `PIO_STORAGE_SOURCES_[NAME]_TYPE=localfs`

Supported Repositories: **model**

When `TYPE` is set to `localfs`, the following configuration keys are
supported.

-   PATH (mandatory)

    File system path at where models are stored, e.g.
    `PIO_STORAGE_SOURCES_FS_PATH=/mymodels`


#### HDFS

Variable Format: `PIO_STORAGE_SOURCES_[NAME]_TYPE=hdfs`

Supported Repositories: **model**

When `TYPE` is set to `hdfs`, the following configuration keys are
supported.

-   PATH (mandatory)

    HDFS path at where models are stored, e.g.
    `PIO_STORAGE_SOURCES_HDFS_PATH=/mymodels`
