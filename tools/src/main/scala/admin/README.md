## Admin API (under development)

### Start Admin HTTP Server

#### For development

NOTE: eleasticsearch and hbase should be running first.

```
$ sbt/sbt "data/compile"
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.tools.admin.AdminRun"
```

#### Start with pio command adminserver

```
$ pio adminserver
```

Admin Server url defaults to: 

#### http://localhost:7071

The host and port can be specified by using the 'ip' and 'port' parameters

```
$ pio adminserver --ip 127.0.0.1 --port 7080
```

### Current supported commands

#### Retrieves the list of current registered applications similar to the command 'pio app list'

http://localhost:7071/cmd/app 

via GET method with no parameters

Returns a JSON array string with all the current applications

#### Creates a new application as 'pio app new'

http://localhost:7071/cmd/app

via POST method with ContentType: application/json and the following post body

{name:'appName',id:'appId',description:'app description'}

#### Delete an application as 'pio app delete \<name\>'

http://localhost:7071/cmd/app/{appName}

via DELETE method with no other parameters

#### Delete application data as 'pio app data-delete \<name\>'

http://localhost:7071/cmd/app/{appName}/data

via DELETE method with no other parameters


