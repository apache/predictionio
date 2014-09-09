"""
Import simple test data for testing time series data
"""
import argparse
import datetime
import predictionio
import pytz
import random
import time

def import_testdata(app_id, api_url):
  client = predictionio.Client(appid=app_id, threads=1, apiurl=api_url)

  # prepare random data
  n = 40
  last_price = 100.0
  eastern = pytz.timezone('US/Eastern')
  last_date = datetime.datetime(2014, 4, 3, 16, 0, 0, 0, eastern)

  day = datetime.timedelta(days=1)

  raw_data = []
  for i in xrange(n):
    last_price += random.normalvariate(mu=0.0, sigma=2.0)
    last_date += day
    since_epoch = long(time.mktime(last_date.timetuple()))
    raw_data.append((since_epoch, last_price))

  # send sync request.
  response = client.set_item(iid="XYZ", properties={"data": raw_data})

  print(response.body)


def main():
  parser = argparse.ArgumentParser(description="some description here..")
  parser.add_argument('--appid', type=int, default=0)
  parser.add_argument('--apiurl', default="http://localhost:8081")

  args = parser.parse_args()
  print args

  import_testdata(
    app_id=args.appid,
    api_url=args.apiurl)

if __name__ == '__main__':
  main()

