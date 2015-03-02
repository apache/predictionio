## Admin API (under development)

### Start Admin HTTP Server

#### For development

NOTE: elasticsearch and hbase should be running first.

```
$ sbt/sbt "tools/compile"
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "tools/run-main io.prediction.tools.admin.AdminRun"
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
## API Doc

### app list:
GET http://localhost:7071/cmd/app

OK Response:
{
  “status”: <STATUS>,
  “message”: <MESSAGE>,
  “apps” : [ 
    { “name': “<APP_NAME>”,
      “id': <APP_ID>,
      “accessKey' : “<ACCESS_KEY>” },
    { “name': “<APP_NAME>”,
      “id': <APP_ID>,
      “accessKey' : “<ACCESS_KEY>” }, ... ]
}

Error Response:
{“status”: <STATUS>, “message” : “<MESSAGE>”}

### app new
POST http://localhost:7071/cmd/app
Request Body:
{ name”: “<APP_NAME>”, // required
  “id”: <APP_ID>, // optional
  “description”: “<DESCRIPTION>” } // optional

OK Response:
{ “status”: <STATUS>,
  “message”: <MESSAGE>,
  “app” : {
    “name”: “<APP_NAME>”,
    “id”: <APP_ID>,
    “accessKey” : “<ACCESS_KEY>” }
}

Error Response:
{ “status”: <STATUS>, “message” : “<MESSAGE>”}

### app delete
DELETE http://localhost:7071/cmd/app/{appName}

OK Response:
{ "status": <STATUS>, "message" : “<MESSAGE>”}

Error Response:
{ “status”: <STATUS>, “message” : “<MESSAGE>”}

### app data-delete
DELETE http://localhost:7071/cmd/app/{appName}/data

OK Response:
{ "status": <STATUS>, "message" : “<MESSAGE>”}

Error Response:
{ “status”: <STATUS>, “message” : “<MESSAGE>” }


### train TBD

#### Training request:
POST http://localhost:7071/cmd/train
Request body: TBD

OK Response: TBD

Error Response: TBD

#### Get training status:
GET http://localhost:7071/cmd/train/{engineInstanceId}

OK Response: TBD
INIT
TRAINING
DONE
ERROR

Error Response: TBD

### deploy TBD


