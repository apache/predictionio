#!/bin/bash - 

set -e

echo "-- Starting eventserver..."
pio eventserver &
echo "-- Waiting for it to finish..."
sleep 10

cp -r /pio_host/testing/simple_scenario/recommendation-engine /
cd /recommendation-engine

echo "-- Adding a new app..."
ACCESS_KEY='ppkprGNCTO_yIejzAdmDqFMOY1XhDiRiX1Da5K6-vbhVrVMoTggzsj2-rrpd4u6U' 
pio app new --access-key $ACCESS_KEY MyApp1

echo "-- Sending some data to eventserver..."
python data/import_eventserver.py --access_key $ACCESS_KEY

echo "-- Building an engine..."
pio build

echo "-- Training a model..."
pio train

echo "-- Deploying an engine..."
pio deploy &
echo "-- Waiting for it to finish..."
sleep 15

echo "-- Sending a query..."
curl -H "Content-Type: application/json" \
-d '{ "user": "1", "num": 4 }' http://localhost:8000/queries.json
