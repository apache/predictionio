How to change layout in Admin server
====================================

When you add / change / remove setings for engines / algorithms, you have to configure the UI in admin server so that can change the settings accordingly. The UI is configured by a json file `dist/conf/init.json`. This doc illustrates how to populate the change and have them show up in admin's UI.

## Instructions
Install [PredictionIO from source](http://docs.prediction.io/current/installation/install-predictionio-from-source.html)

````
# Start admin server in the distribution directory.
cd dist/target/PredictionIO-<version>/
bin/start-admin.sh

# Edit init.json in the repository
vim ../../conf/init.json

# Copy it to the distribution directory. Unless you know what you are doing, don't edit conf/init.json directly in the distribution directory.
cp ../../conf/init.json conf/init.json

# Populate the change
bin/setup.sh

# After populating, you will see your change in Admin server.
````

## Troubleshooting
`setup.sh` fails if the json file is malformed. You can use python's json package for error checking: `python -mjson.tool ../../conf/init.json`.
