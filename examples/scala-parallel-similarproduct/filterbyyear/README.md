# Similar Product Template

## Documentation

Please refer to http://docs.prediction.io/templates/similarproduct/quickstart/

## Versions

### develop

### v0.1.1

- Persist RDD to memory (.cache()) in DataSource for better performance
- Use local model for faster serving.

### v0.1.0

- initial version


## Development Notes

### import sample data

```
$ python data/import_eventserver.py --access_key <your_access_key>
```

### query

normal:

```
curl -H "Content-Type: application/json" \
-d '{ "items": ["i1", "i3", "i10", "i2", "i5", "i31", "i9"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "items": ["i1", "i3", "i10", "i2", "i5", "i31", "i9"],
  "num": 10,
  "categories" : ["c4", "c3"]
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "items": ["i1", "i3", "i10", "i2", "i5", "i31", "i9"],
  "num": 10,
  "whiteList": ["i21", "i26", "i40"]
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

```
curl -H "Content-Type: application/json" \
-d '{
  "items": ["i1", "i3", "i10", "i2", "i5", "i31", "i9"],
  "num": 10,
  "blackList": ["i21", "i26", "i40"]
}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```

unknown item:

```
curl -H "Content-Type: application/json" \
-d '{ "items": ["unk1", "i3", "i10", "i2", "i5", "i31", "i9"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```


all unknown items:

```
curl -H "Content-Type: application/json" \
-d '{ "items": ["unk1", "unk2", "unk3", "unk4"], "num": 10}' \
http://localhost:8000/queries.json \
-w %{time_connect}:%{time_starttransfer}:%{time_total}
```
