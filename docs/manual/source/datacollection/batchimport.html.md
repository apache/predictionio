---
title: Importing Data in Batch
---

If you have a large amount of data to start with, performing batch import will
be much faster than sending every event over an HTTP connection.

## Preparing the Input File

The import tool expects its input to be a file stored either in the local
filesystem or on HDFS. Each line of the file should be a JSON object string
representing an event. For more information about the format of event JSON
object, please refer to [this page](/datacollection/eventapi/#using-event-api).

Shown below is an example that contains 5 events ready to be imported to the
Event Server.

```json
{"event":"buy","entityType":"user","entityId":"3","targetEntityType":"item","targetEntityId":"0","eventTime":"2014-11-21T01:04:14.716Z"}
{"event":"buy","entityType":"user","entityId":"3","targetEntityType":"item","targetEntityId":"1","eventTime":"2014-11-21T01:04:14.722Z"}
{"event":"rate","entityType":"user","entityId":"3","targetEntityType":"item","targetEntityId":"2","properties":{"rating":1.0},"eventTime":"2014-11-21T01:04:14.729Z"}
{"event":"buy","entityType":"user","entityId":"3","targetEntityType":"item","targetEntityId":"7","eventTime":"2014-11-21T01:04:14.735Z"}
{"event":"buy","entityType":"user","entityId":"3","targetEntityType":"item","targetEntityId":"8","eventTime":"2014-11-21T01:04:14.741Z"}
```

WARNING: Please make sure your import file does not contain any empty lines.
Empty lines will be treated as a null object and will return an error during
import.

## Import Events from the Input File

Importing events from a file can be done easily using the command line
interface. Assuming that `pio` be in your search path, your App ID be `123`, and
the input file `events.json` be in your current working directory:

```bash
$ pio import --appid 123 --input events.json
```

After a brief while, the tool should return to the console without any error.
Congratulations! You have successfully imported your events.
