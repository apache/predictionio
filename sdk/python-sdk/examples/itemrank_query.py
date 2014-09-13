"""
Import simple query for itemrank
"""
import predictionio
import argparse
import time


def send_queries(url):
  client = predictionio.PredictionClient(threads=1, url=url)

  # Sync Query
  query = {
      "uid": "u2",
      "iids": ["i0", "i1", "i2", "i3"],
      }
  response = client.send_query(query)
  print(response)

  # Async Query
  query = {
      "uid": "u9527",
      "iids": ["i0", "i1", "i2", "i3"],
      }
  response = client.asend_query(query)
  print(response.get_response())

  client.close()


def main():
  parser = argparse.ArgumentParser(
      description="Sample sdk for PredictionClient")
  parser.add_argument('--url', default="http://localhost:8000")

  args = parser.parse_args()
  print args

  send_queries(url=args.url)


if __name__ == '__main__':
  main()
