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

SP500_LIST = [
    "A", "AA", "AAPL", "ABBV", "ABC", "ABT", "ACE", "ACN", "ACT", "ADBE", "ADI",
    "ADM", "ADP", "ADS", "ADSK", "ADT", "AEE", "AEP", "AES", "AET", "AFL",
    "AGN", "AIG", "AIV", "AIZ", "AKAM", "ALL", "ALLE", "ALTR", "ALXN", "AMAT",
    "AME", "AMGN", "AMP", "AMT", "AMZN", "AN", "AON", "APA", "APC", "APD",
    "APH", "ARG", "ATI", "AVB", "AVP", "AVY", "AXP", "AZO", "BA", "BAC", "BAX",
    "BBBY", "BBT", "BBY", "BCR", "BDX", "BEAM", "BEN", "BF-B", "BHI", "BIIB",
    "BK", "BLK", "BLL", "BMS", "BMY", "BRCM", "BRK-B", "BSX", "BTU", "BWA",
    "BXP", "C", "CA", "CAG", "CAH", "CAM", "CAT", "CB", "CBG", "CBS", "CCE",
    "CCI", "CCL", "CELG", "CERN", "CF", "CFN", "CHK", "CHRW", "CI", "CINF",
    "CL", "CLX", "CMA", "CMCSA", "CME", "CMG", "CMI", "CMS", "CNP", "CNX",
    "COF", "COG", "COH", "COL", "COP", "COST", "COV", "CPB", "CRM", "CSC",
    "CSCO", "CSX", "CTAS", "CTL", "CTSH", "CTXS", "CVC", "CVS", "CVX", "D",
    "DAL", "DD", "DE", "DFS", "DG", "DGX", "DHI", "DHR", "DIS", "DISCA", "DLPH",
    "DLTR", "DNB", "DNR", "DO", "DOV", "DOW", "DPS", "DRI", "DTE", "DTV", "DUK",
    "DVA", "DVN", "EA", "EBAY", "ECL", "ED", "EFX", "EIX", "EL", "EMC", "EMN",
    "EMR", "EOG", "EQR", "EQT", "ESRX", "ESS", "ESV", "ETFC", "ETN", "ETR",
    "EW", "EXC", "EXPD", "EXPE", "F", "FAST", "FB", "FCX", "FDO", "FDX", "FE",
    "FFIV", "FIS", "FISV", "FITB", "FLIR", "FLR", "FLS", "FMC", "FOSL", "FOXA",
    "FRX", "FSLR", "FTI", "FTR", "GAS", "GCI", "GD", "GE", "GGP", "GHC", "GILD",
    "GIS", "GLW", "GM", "GMCR", "GME", "GNW", "GOOG", "GOOGL", "GPC", "GPS",
    "GRMN", "GS", "GT", "GWW", "HAL", "HAR", "HAS", "HBAN", "HCBK", "HCN",
    "HCP", "HD", "HES", "HIG", "HOG", "HON", "HOT", "HP", "HPQ", "HRB", "HRL",
    "HRS", "HSP", "HST", "HSY", "HUM", "IBM", "ICE", "IFF", "IGT", "INTC",
    "INTU", "IP", "IPG", "IR", "IRM", "ISRG", "ITW", "IVZ", "JBL", "JCI", "JEC",
    "JNJ", "JNPR", "JOY", "JPM", "JWN", "K", "KEY", "KIM", "KLAC", "KMB", "KMI",
    "KMX", "KO", "KORS", "KR", "KRFT", "KSS", "KSU", "L", "LB", "LEG", "LEN",
    "LH", "LLL", "LLTC", "LLY", "LM", "LMT", "LNC", "LO", "LOW", "LRCX", "LSI",
    "LUK", "LUV", "LYB", "M", "MA", "MAC", "MAR", "MAS", "MAT", "MCD", "MCHP",
    "MCK", "MCO", "MDLZ", "MDT", "MET", "MHFI", "MHK", "MJN", "MKC", "MMC",
    "MMM", "MNST", "MO", "MON", "MOS", "MPC", "MRK", "MRO", "MS", "MSFT", "MSI",
    "MTB", "MU", "MUR", "MWV", "MYL", "NBL", "NBR", "NDAQ", "NE", "NEE", "NEM",
    "NFLX", "NFX", "NI", "NKE", "NLSN", "NOC", "NOV", "NRG", "NSC", "NTAP",
    "NTRS", "NU", "NUE", "NVDA", "NWL", "NWSA", "OI", "OKE", "OMC", "ORCL",
    "ORLY", "OXY", "PAYX", "PBCT", "PBI", "PCAR", "PCG", "PCL", "PCLN", "PCP",
    "PDCO", "PEG", "PEP", "PETM", "PFE", "PFG", "PG", "PGR", "PH", "PHM", "PKI",
    "PLD", "PLL", "PM", "PNC", "PNR", "PNW", "POM", "PPG", "PPL", "PRGO", "PRU",
    "PSA", "PSX", "PVH", "PWR", "PX", "PXD", "QCOM", "QEP", "R", "RAI", "RDC",
    "REGN", "RF", "RHI", "RHT", "RIG", "RL", "ROK", "ROP", "ROST", "RRC", "RSG",
    "RTN", "SBUX", "SCG", "SCHW", "SE", "SEE", "SHW", "SIAL", "SJM", "SLB",
    "SLM", "SNA", "SNDK", "SNI", "SO", "SPG", "SPLS", "SRCL", "SRE", "STI",
    "STJ", "STT", "STX", "STZ", "SWK", "SWN", "SWY", "SYK", "SYMC", "SYY", "T",
    "TAP", "TDC", "TE", "TEG", "TEL", "TGT", "THC", "TIF", "TJX", "TMK", "TMO",
    "TRIP", "TROW", "TRV", "TSCO", "TSN", "TSO", "TSS", "TWC", "TWX", "TXN",
    "TXT", "TYC", "UNH", "UNM", "UNP", "UPS", "URBN", "USB", "UTX", "V", "VAR",
    "VFC", "VIAB", "VLO", "VMC", "VNO", "VRSN", "VRTX", "VTR", "VZ", "WAG",
    "WAT", "WDC", "WEC", "WFC", "WFM", "WHR", "WIN", "WLP", "WM", "WMB", "WMT",
    "WU", "WY", "WYN", "WYNN", "X", "XEL", "XL", "XLNX", "XOM", "XRAY", "XRX",
    "XYL", "YHOO", "YUM", "ZION", "ZMH", "ZTS"]

ETF_LIST = ["QQQ", "SPY", "XLY", "XLP", "XLE", "XLF", "XLV", 
    "XLI", "XLB", "XLK", "XLU"]


def since_epoch(dt):
  return (dt - EPOCH).total_seconds()


def import_data(client, app_id, ticker, start_time, end_time, event_time):
  print "Importing:", ticker, start_time, end_time

  try:
    df = pdata.DataReader(ticker, 'yahoo', start_time, end_time)
  # TODO: handle error, e.g. response code not 200

    print "Extracted:", df.index[0], df.index[-1]
  except IOError, ex:
    print ex
    print "Data not exist. Returning"
    return

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
      'appId': app_id,
      'eventTime': datetime.isoformat(event_time.replace(microsecond=1)) + 'Z',
      }

  response = client.create_event(data)
  print(response.body)


def import_all():
  time_slices = [
      (datetime(1999, 1, 1), datetime(2004, 1, 1), datetime(2004, 1, 2)),
      (datetime(2004, 1, 1), datetime(2009, 1, 1), datetime(2009, 1, 2)),
      (datetime(2009, 1, 1), datetime(2014, 9, 1), datetime(2014, 9, 2)),
      ]

  app_id = 2
  url = 'http://localhost:7070'
  client = predictionio.DataClient(app_id=app_id, threads=1, data_url=url)

  tickers = SP500_LIST + ETF_LIST 

  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, app_id, ticker, 
          time_slice[0], time_slice[1], time_slice[2])



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
 
  app_id = 1
  url = 'http://localhost:7070'
  client = predictionio.DataClient(app_id=app_id, threads=1, data_url=url)

  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, app_id, ticker, 
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
      import_data(client, app_id, ticker, 
          time_slice[0], time_slice[1], time_slice[2])

  time_slices = [
      (datetime(2014, 1, 10), datetime(2014, 2, 20), datetime(2014, 2, 28)),
      (datetime(2014, 2, 10), datetime(2014, 3, 31), datetime(2014, 4, 2)),
      ]
  tickers = ['FB']
  for ticker in tickers:
    for time_slice in time_slices:
      import_data(client, app_id, ticker, 
          time_slice[0], time_slice[1], time_slice[2])





def import_one():
  start_time = datetime(2014, 1, 1)
  end_time = datetime(2014, 3, 1)
  event_time = datetime(2014, 4, 1)
  ticker = 'GOOG'
 
  app_id = 1
  url = 'http://localhost:7070'
  client = predictionio.DataClient(app_id=app_id, threads=1, data_url=url)

  import_data(client, app_id, ticker, start_time, end_time, event_time)

if __name__ == '__main__':
  import_all()
  #import_predefined()

