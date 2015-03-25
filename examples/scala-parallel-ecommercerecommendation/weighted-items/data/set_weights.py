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
