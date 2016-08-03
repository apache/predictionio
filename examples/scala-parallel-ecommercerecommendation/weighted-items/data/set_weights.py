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
Set weights for E-Commerce Recommendation Engine Template items
"""

import argparse
import predictionio

def set_weights(client):
    client.create_event(
        event="$set",
        entity_type="constraint",
        entity_id="weightedItems",
        properties={
            "weights": [
                {
                    "items": ["i4", "i14"],
                    "weight": 1.2
                },
                {
                    "items": ["i11"],
                    "weight": 1.5
                }
            ]
        }
    )

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Set weights to items")
    parser.add_argument('--access_key', default='invalid_access_key')
    parser.add_argument('--url', default="http://localhost:7070")

    args = parser.parse_args()

    client = predictionio.EventClient(
        access_key=args.access_key,
        url=args.url,
        threads=5,
        qsize=500)
    set_weights(client)
