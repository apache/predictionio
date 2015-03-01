# Recommendation Template

## Documentation

Please refer to http://docs.prediction.io/templates/recommendation/quickstart/

## Versions

### develop

- TODO

### v0.1.1

- Persist RDD to memory (.cache()) in DataSource for better performance and quick fix for new user/item ID BiMap error issue.

### v0.1.0

- initial version
- known issue:
  * If importing new events of new users/itesm during training, the new user/item id can't be found in the BiMap.
