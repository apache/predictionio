# Recommended User Template

This example is based on version v0.1.3 of the Similar Product Engine Template. The Similar Product Engine Template has been customized to recommend users instead of items.

The main difference from the original template is the following:

Instead of using user-to-item events to find similar items, user-to-user events are used to find similar users you may also like to view, follow, etc (depending on which events are used in training). By default, view events are used.

## Overview

This engine template recommends users that are "similar" to other users.
Similarity is not defined by the user's attributes but by the user's previous actions. By default, it uses the 'view' action such that user A and B are considered similar if most users who view A also view B.

This template is ideal for recommending users to other users based on their recent actions.
Use the IDs of the recently viewed users of a customer as the *Query*,
the engine will predict other users that this customer may also like.

This approach works perfectly for customers who are **first-time visitors** or have not signed in.
Recommendations are made dynamically in *real-time* based on the most recent user preference you provide in the *Query*.
You can, therefore, recommend users to visitors without knowing a long history about them.

One can also use this template to build the popular feature of "people you may also like to view, follow, etc** quickly by provide similar users to what you have just viewed or liked.

## Usage

### Event Data Requirements

By default, this template takes the following data from Event Server as Training Data:

- User *$set* events
- User *view* User events

### Input Query

- List of UserIDs, which are the targeted users
- N (number of users to be recommended)
- List of white-listed UserIds (optional)
- List of black-listed UserIds (optional)

The template also supports black-list and white-list. If a white-list is provided, the engine will include only those users in its recommendation.
Likewise, if a black-list is provided, the engine will exclude those users in its recommendation.

## Documentation

Please refer to http://docs.prediction.io/templates/similarproduct/quickstart/

## Versions

### v0.1.0

- initial version

## Development Notes

### import sample data

```
$ python data/import_eventserver.py --access_key <your_access_key>
```

### sample queries

normal:

```
curl -H "Content-Type: application/json" \
-d '{ "users": ["u1", "u3", "u10", "u2", "u5", "u31", "u9"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "users": ["u1", "u3", "u10", "u2", "u5", "u31", "u9"],
  "num": 10
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "users": ["u1", "u3", "u10", "u2", "u5", "u31", "u9"],
  "num": 10,
  "whiteList": ["u21", "u26", "u40"]
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "users": ["u1", "u3", "u10", "u2", "u5", "u31", "u9"],
  "num": 10,
  "blackList": ["u21", "u26", "u40"]
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

unknown user:

```
curl -H "Content-Type: application/json" \
-d '{ "users": ["unk1", "u3", "u10", "u2", "u5", "u31", "u9"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```


all unknown users:

```
curl -H "Content-Type: application/json" \
-d '{ "users": ["unk1", "unk2", "unk3", "unk4"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```
