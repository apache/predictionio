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

SEED = 3

def import_events(client):
  random.seed(SEED)
  count = 0
  print "Importing data..."

  # generate 10 users, with user uid1,2,....,10
  # with some random attributes
  user_ids = [ ("uid"+str(i)) for i in range(1, 11)]
  for user_id in user_ids:
    print "Set user", user_id
    client.create_event(
      event="$set",
      entity_type="user",
      entity_id=user_id,
      properties={
        "attr0" : float(random.randint(0, 4)),
        "attr1" : random.randint(10, 14),
        "attr2" : random.randint(20, 24)
       }
    )
    count += 1

  # generate 50 items, with iid1,2,....,50
  # with some randome attributes
  item_ids = [ ("iid"+str(i)) for i in range(1, 51)]
  for item_id in item_ids:
    print "Set item", item_id
    client.create_event(
      event="$set",
      entity_type="item",
      entity_id=item_id,
      properties={
        "attrA" : random.choice(["something1", "something2", "valueX"]),
        "attrB" : random.randint(10, 30),
        "attrC" : random.choice([True, False])
       }
    )
    count += 1

  # each user randomly rate or buy 10 items
  for user_id in user_ids:
    for viewed_item in random.sample(item_ids, 10):
      if (random.randint(0, 1) == 1):
        print "User", user_id ,"rates item", viewed_item
        client.create_event(
          event="rate",
          entity_type="user",
          entity_id=user_id,
          target_entity_type="item",
          target_entity_id=item_id,
          properties= { "rating" : float(random.randint(1, 6)) }
        )
      else:
        print "User", user_id ,"buys item", viewed_item
        client.create_event(
          event="buy",
          entity_type="user",
          entity_id=user_id,
          target_entity_type="item",
          target_entity_id=item_id
        )
      count += 1

  print "%s events are imported." % count

if __name__ == '__main__':
  parser = argparse.ArgumentParser(
    description="Import sample data for recommendation engine")
  parser.add_argument('--access_key', default='invald_access_key')
  parser.add_argument('--url', default="http://localhost:7070")

  args = parser.parse_args()
  print args

  client = predictionio.EventClient(
    access_key=args.access_key,
    url=args.url,
    threads=5,
    qsize=500)
  import_events(client)
