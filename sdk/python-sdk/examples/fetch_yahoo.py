"""
Import historical stock data from yahoo finance.
"""

import argparse
from datetime import datetime
import predictionio
import pytz
import time
from pandas.io import data as pdata
import numpy

EPOCH = datetime(1970, 1, 1, tzinfo=pytz.utc)


def since_epoch(dt):
  return (dt - EPOCH).total_seconds()


def import_data(client, appid, ticker, start_time, end_time, event_time):
  print "Importing:", ticker, start_time, end_time
  df = pdata.DataReader(ticker, 'yahoo', start_time, end_time)
  # TODO: handle error, e.g. response code not 200

  # assume we only extract US data
  eastern = pytz.timezone('US/Eastern')

  columns = [
      ('Open', 'open'),
      ('High', 'high'),
      ('Low', 'low'),
      ('Close', 'close'),
      ('Volume', 'volume'),
      ('Adj Close', 'adjclose')]

  yahoo_data = dict()
  yahoo_data['ticker'] = ticker
  yahoo_data['t'] = [
      # hour=16 to indicate market close time
      since_epoch(eastern.localize(date_.to_pydatetime().replace(hour=16)))
      for date_ in df.index]

  for column in columns:
    yahoo_data[column[1]] = map(numpy.asscalar, df[column[0]].values)

  properties = {'yahoo': yahoo_data}

  data = {
      'event': '$set',
      'entityType': 'yahoo',
      'entityId': ticker,
      'properties': properties,
      'appId': appid,
      'eventTime': datetime.isoformat(event_time.replace(microsecond=1)) + 'Z',
      }

  response = client.create_event(data)
  print(response.body)


def import_predefined():
  # time_slices is discontinuted
  # startTime, endTime, eventDate
  time_slices = [
      (datetime(2013, 12, 1), datetime(2014, 2, 1), datetime(2014, 2, 2)),
      (datetime(2014, 1, 1), datetime(2014, 1, 20), datetime(2014, 2, 10)),
      (datetime(2014, 1, 10), datetime(2014, 2, 20), datetime(2014, 2, 28)),
      (datetime(2014, 2, 10), datetime(2014, 3, 31), datetime(2014, 4, 2)),
      (datetime(2014, 5, 1), datetime(2014, 6, 15), datetime(2014, 6, 20)),
      (datetime(2014, 6, 1), datetime(2014, 7, 1), datetime(2014, 7, 15)),
      ]

  tickers = ['SPY', 'AAPL', 'IBM', 'MSFT']
 
  appid = 1
  apiurl = 'http://localhost:7070'
  client = predictionio.Client(appid=appid, threads=1, apiurl=apiurl)

  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, appid, ticker, 
          time_slice[0], time_slice[1], time_slice[2])

  # below are data with holes
  time_slices = [
      (datetime(2014, 1, 1), datetime(2014, 1, 20), datetime(2014, 2, 10)),
      (datetime(2014, 2, 10), datetime(2014, 3, 31), datetime(2014, 4, 2)),
      (datetime(2014, 6, 1), datetime(2014, 7, 1), datetime(2014, 7, 15)),
      ]

  tickers = ['AMZN']
  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, appid, ticker, 
          time_slice[0], time_slice[1], time_slice[2])

  time_slices = [
      (datetime(2014, 1, 10), datetime(2014, 2, 20), datetime(2014, 2, 28)),
      (datetime(2014, 2, 10), datetime(2014, 3, 31), datetime(2014, 4, 2)),
      ]
  tickers = ['FB']
  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, appid, ticker, 
          time_slice[0], time_slice[1], time_slice[2])





def import_one():
  start_time = datetime(2014, 1, 1)
  end_time = datetime(2014, 2, 1)
  ticker = 'AAPL'
 
  appid = 1
  apiurl = 'http://localhost:8081'
  client = predictionio.Client(appid=appid, threads=1, apiurl=apiurl)

  import_data(client, appid, ticker, start_time, end_time)

if __name__ == '__main__':
  import_predefined()

