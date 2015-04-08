# Recommended User Template

## Versions

### v0.1.0

- initial version

## Development Notes

### import sample data

```
$ python data/import_eventserver.py --access_key <your_access_key>
```

### sample query

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
