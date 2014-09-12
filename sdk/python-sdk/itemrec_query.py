"""
Import simple query for getting itemrank
"""
import predictionio
import argparse
import time


def send_queries(apiurl):
  client = predictionio.PredictionClient(threads=1, apiurl=apiurl)

  # Sync Query
  query = {
      "uid": "u2",
      "items": ["i0", "i1", "i2", "i3"],
      }
  response = client.send_query(query)
  print(response)

  # Async Query
  query = {
      "uid": "u9527",
      "items": ["i0", "i1", "i2", "i3"],
      }
  response = client.asend_query(query)
  print(response.get_response())

  client.close()


def main():
  parser = argparse.ArgumentParser(description="some description here..")
  parser.add_argument('--apiurl', default="http://localhost:8000")

  args = parser.parse_args()
  print args

  send_queries(apiurl=args.apiurl)

if __name__ == '__main__':
  main()

