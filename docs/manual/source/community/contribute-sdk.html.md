---
title:  Contribute a SDK
---

A SDK should provide convenient methods for client applications to easily 
record users' behaviors in PredictionIO's Event Server and also query
recommendations from the ML Engines. Therefore, a SDK typically has 2
corresponding clients: `Event Client` and `Engine Client`.

The following guideline bases on the REST API provided by PredictionIO's 
Event Client which details can be found [here](http://docs.prediction.io/datacollection/eventapi/).


## Event Client
Because the Event Server has only 1 connection point, the `Event Client`
needs to implement this core request first. The core request has the 
following rules.

- **URL**: `<base URL>/events.json?accessKey=<your access key>` 
(e.g. http://localhost:7070/events.json?accessKey=1234567890)

- **Request**: `POST` + JSON data. Please refer to the [Event Creation API]
(http://docs.prediction.io/datacollection/eventapi/) for the details 
on the fields of the JSON data object.

- **Response**: status code `201` on success with a JSON result containing
the event ID.

Other convenient methods are just shortcut. They could simply build 
the event's parameters and call the core request. `Event Client` should 
support the following 7 shorthand operations:

- **User entities**
    + **Sets properties of a user**: with the JSON object

        ```json
        {
            'event': '$set',
            'entityType': 'user',
            'entityId': <user_ID>,
            'properties': <properties>
        }
        ```

    + **Unsets some properties of a user**: with the JSON object

        ```json
        {
            'event': '$unset',
            'entityType': 'user',
            'entityId': <user_ID>,
            'properties': <properties>
        }
        ```

    + **Delete a user**: with the JSON object

        ```json
        {
            'event': '$delete',
            'entityType': 'user',
            'entityId': <user_ID>
        }
        ```

- **Item entities**
    + **Sets properties of an item**: with the JSON object

        ```json
        {
            'event': '$set',
            'entityType': 'item',
            'entityId': <item_ID>,
            'properties': <properties>
        }
        ```

    + **Unsets some properties of an item**: with the JSON object

        ```json
        {
            'event': '$unset',
            'entityType': 'item',
            'entityId': <item_ID>,
            'properties': <properties>
        }
        ```

    + **Delete an item**: with the JSON object

        ```json
        {
            'event': '$delete',
            'entityType': 'item',
            'entityId': <item_ID>
        }
        ```

- **Others**
    + **Record a user's action on some item**: with the JSON object

        ```json
        {
            'event': <event_name>,
            'entityType': 'user',
            'entityId': <user_ID>,
            'targetEntityType': 'item',
            'targetEntityId': <item_ID>,
            'properties': <properties>
        }
        ```

Again, please refer to the [API documentation]
(http://docs.prediction.io/datacollection/eventapi/) for explanations
on the reversed events like `$set`, `$unset` or `$delete`.

INFO: The `eventTime` is optional but it is recommended that the client
application should include time in the request. Therefore, it is best
that the `Event Client` includes the time field if missing, before
sending the event to the server.


## Engine Client
`Engine Client`'s main job is to retrieve recommendation or prediction 
results from PredictionIO's Engines. It has only a few rules on the 
request and response type.

- **URL**: `<base URL>/queries.json` (e.g. http://localhost:8000/queries.json)
- **Request**: `POST` + JSON data
- **Response**: status code `200` on success with JSON result which
format is defined by the PredictionIO's Serving component.

## Testing Your SDK
You can set up a local host PredictionIO environment to test your SDK.
However, it is hard to set it up online to test your SDK automatically 
using services like Travis CI. In that case, you should consider
using these lightweight [mock servers]
(https://github.com/minhtule/PredictionIO-Mock-Server). Please see the
instructions in the repo how to use it. It takes less than 5 minutes!

That's it! We are looking forward to see your SDK!
