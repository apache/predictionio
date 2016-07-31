#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Import sample data for recommendation engine
"""

import predictionio
import argparse
import random

ITEM_ACTIONS_DELIMITER = "|"
RATE_ACTIONS_DELIMITER = "\t"
SEED = 3

GENRES = ["unknown", "action", "adventure", "animation", "children's", "comedy","crime", "documentary", "drama",
          "fantasy", "film-noir", "horror", "musical", "mystery", "romance", "sci-fi", "thriller", "war", "western"]

def import_events(client, items_file, ratings_file):
  random.seed(SEED)

  f = open(items_file, 'r')
  print "Importing items..."
  items = 0
  for line in f:
    data = line.rstrip('\r\n').split(ITEM_ACTIONS_DELIMITER)
    id = data[0]
    genres_str = data[5:]
    genres_bool = [bool(int(g)) for g in genres_str]
    genres = [g for b, g in zip(genres_bool, GENRES) if b]
    client.create_event(
      event="$set",
      entity_type="item",
      entity_id=id,
      properties= { "categories" : genres }
    )
    items += 1
  print "%s items are imported." % items
  f.close()

  f = open(ratings_file, 'r')
  print "Importing ratings..."
  ratings = 0
  for line in f:
    data = line.rstrip('\r\n').split(RATE_ACTIONS_DELIMITER)
    # For demonstration purpose, randomly mix in some buy events
    if random.randint(0, 1) == 1:
      client.create_event(
        event="rate",
        entity_type="user",
        entity_id=data[0],
        target_entity_type="item",
        target_entity_id=data[1],
        properties= { "rating" : float(data[2]) }
      )
    else:
      client.create_event(
        event="buy",
        entity_type="user",
        entity_id=data[0],
        target_entity_type="item",
        target_entity_id=data[1]
      )
    ratings += 1
  f.close()
  print "%s ratings are imported." % ratings

if __name__ == '__main__':
  parser = argparse.ArgumentParser(
    description="Import sample data for recommendation engine")
  parser.add_argument('--access_key', default='invald_access_key')
  parser.add_argument('--url', default="http://localhost:7070")
  parser.add_argument('--ratings_file')
  parser.add_argument('--items_file')

  args = parser.parse_args()
  print args

  client = predictionio.EventClient(
    access_key=args.access_key,
    url=args.url,
    threads=5,
    qsize=500)
  import_events(client, args.items_file, args.ratings_file)
